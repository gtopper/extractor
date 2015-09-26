package com.topper.plugin

import com.topper.logging.Logging._
import scala.tools.nsc.Global

/*
 * TODO: turn this comment into an actionable such as logging the flags for forensics,
 *       a ticket, documentation, or whatever. 
 * 
 * Note: it is documented in the source, that 
 * a flag settings.Yshowsymowners, adds the symbol owner's id to the nameString.
 * In case this is so, owner chains may be obtained more cheaply. Also note that
 * some settings may skew the results of this code.
 */

object TraversalExtractionWrapper {
  def apply(global: Global)(unit: global.CompilationUnit)(projectName: String): Graph = {
    val graph = TraversalExtraction(global)(unit.body)

    Log(s"${graph.nodes.size} entities (before deduplication), ${graph.edges.size} edges")
    Log(s"Done examining source file ${unit.source.path} containing " +
      s"${graph.nodes.size} entities (before deduplication), ${graph.edges.size} edges")

    graph
  }
}

object TraversalExtraction {

  def apply(global: Global)(body: global.Tree): Graph = {

    // for having access to typed symbol methods
    import global._

    var nodes: Vector[Node] = Vector.empty

    def addNode(global: Global)(s: global.Symbol): Node = {

      val filename = Option(s.sourceFile).map(_.toString)

      val newNode = Node(
        s.id,
        s.nameString,
        s.owner.nameString,
        s.kindString,
        s.isSynthetic,
        filename
      )

      nodes = nodes :+ newNode

      newNode
    }

    var edges: Vector[Edge] = Vector.empty

    def addEdge(id1: Int, edgeKind: String, id2: Int): Unit =
      edges = edges :+ Edge(id1, edgeKind, id2)

    /*
     * Captures the node's hierarchy chain -
     * this is needed for the case that the node is a library symbol,
     * so we won't (necessarily) bump into its parents while compiling
     * the project being compiled.
     */
    def recordOwnerChain(node: Node, symbol: Symbol): Unit = {
      // Note: there is also the reflection library supplied Node.ownerChain method,
      //       for now, the recursive iteration used here instead seems as good.
      if (!node.ownersTraversed) {
        if (symbol.nameString != "<root>") {
          val ownerSymbol = symbol.owner
          val ownerNode = addNode(global)(ownerSymbol)
          addEdge(symbol.owner.id, "is member of", symbol.id)
          recordOwnerChain(ownerNode, ownerSymbol)
          node.ownersTraversed = true
        }
      }
    }

    class ExtractionTraversal(defParent: Option[global.Symbol]) extends Traverser {
      override def traverse(tree: Tree): Unit = {

        // see http://www.scala-lang.org/api/2.11.0/scala-reflect/index.html#scala.reflect.api.Trees
        // for the different cases, as well as the source of the types matched against
        tree match {

          // capture member usage
          case select: Select =>

            if (defParent.isDefined) addEdge(defParent.get.id, "uses", tree.symbol.id)

            val node = addNode(global)(select.symbol)

            select.symbol.kindString match {
              case "method" | "constructor" =>
                if (defParent.isEmpty) Warning.logMemberParentLacking(global)(select.symbol)

              case _ =>
            }

            recordOwnerChain(node, select.symbol)

          /*
           *    See:
           *    https://groups.google.com/d/topic/scala-internals/Ms9WUAtokLo/discussion
           *    https://groups.google.com/forum/#!topic/scala-internals/noaEpUb6uL4
           */
          case ident: Ident => Log("ignoring Ident: " + ident.symbol)

          // Capture val definitions (rather than their automatic accessor methods..)
          case ValDef(mods: Modifiers, name: TermName, tpt: Tree, rhs: Tree) =>

            val symbol = tree.symbol

            addNode(global)(symbol)
            addEdge(symbol.id, "is member of", defParent.get.id)

            // Capturing the defined val's type (not kind) while at it
            val valueType = symbol.tpe.typeSymbol // the type that this val instantiates.
          val node = addNode(global)(valueType)
            recordOwnerChain(node, valueType)

            addEdge(symbol.id, "is of type", valueType.id)

          // Capture defs of methods.
          // Note this will also capture default constructors synthesized by the compiler
          // and synthetic accessor methods defined by the compiler for vals
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val symbol = tree.symbol

            addNode(global)(symbol)
            addEdge(symbol.id, "is member of", defParent.get.id)

            val traverser = new ExtractionTraversal(Some(tree.symbol))
            if (symbol.nameString == "get") {
              //val tracer = new TraceTree
              //tracer.traverse(tree)
              //Log(Console.RED + Console.BOLD + showRaw(rhs))
              //Log(symbol.tpe.typeSymbol)
            }
            traverser.traverse(rhs)

          // Capture type definitions (classes, traits, objects)
          case Template(parents, self, body) =>

            val typeSymbol = tree.tpe.typeSymbol

            val node = addNode(global)(typeSymbol)
            recordOwnerChain(node, typeSymbol)

            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach {s =>
              val parentNode = addNode(global)(s)
              recordOwnerChain(parentNode, s)
            }

            // Throw this check away if it hasn't written to the console for a while
            if (defParent.isDefined)
              if (defParent.get.id != typeSymbol.owner.id)
                Warning.logParentNotOwner(global)(defParent.get, typeSymbol.owner)

            parentTypeSymbols.foreach(s =>
              addEdge(typeSymbol.id, "extends", s.id))

            val traverser = new ExtractionTraversal(Some(tree.tpe.typeSymbol))
            body foreach {tree => traverser.traverse(tree)}

          case subtree =>
            super.traverse(subtree)
        }
      }
    }

    val traverser = new ExtractionTraversal(None)
    traverser.traverse(body)

    Graph(nodes, edges)
  }
}

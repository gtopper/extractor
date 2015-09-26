package Extractor.Plugin

import scala.tools.nsc.Global

object Nodes {

  var list: List[Node] = List.empty

  def apply(global: Global)(s: global.Symbol): Node = {

    val newNode = {
      val (source, filename) = s.sourceFile match {
        case null => // no source file included in this project for this entity
          None -> None
        case _ =>
          SourceExtract(global)(s) -> Some(s.sourceFile.toString)
      }

      Node(s.id, s.nameString, s.owner.nameString, s.kindString, !s.isSynthetic, source, filename)
    }

    list = newNode :: list

    newNode
  }
}

object Edges {

  var list: List[Edge] = List()

  def apply(id1: Int, edgeKind: String, id2: Int): Unit =
    list = Edge(id1, edgeKind, id2) :: list
}

case class Edge(id1: Int,
                edgeKind: String,
                id2: Int)

case class Node(id: Int,
                name: String,
                owner: String,
                kind: String,
                notSynthetic: Boolean,
                source: Option[String],
                fileName: Option[String]) {
  var ownersTraversed = false
}

case class Graph(nodes: Vector[Node], edges: Vector[Edge]) {

  def +(that: Graph): Graph = Graph(this.nodes ++ that.nodes, this.edges ++ that.edges)
}

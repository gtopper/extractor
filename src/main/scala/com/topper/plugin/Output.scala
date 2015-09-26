package com.topper.plugin

import com.topper.util.FileIO._

object Output {

  def quote(int: Int) = "\"" + int.toString + "\""

  def write(graph: Graph) = {
    Log("writing extracted type relations and call graph...")
    val projectName = PluginArgs.projectName

    val nodes = graph.nodes
    val edges = graph.edges.distinct
    val distinctEdges = edges.distinct

    val nodesByID = nodes.map(node => node.id -> node).toMap

    Log(s"Collected ${nodes.size} nodes (${nodesByID.size} distinct).")
    Log(s"Collected ${edges.size} edges (${distinctEdges.size} distinct).")

    writeOutputFile(projectName, "nodes",
      "definition,notSynthetic,id,name,kind\n" +
        nodesByID.map {node =>
          List(
            node._2.notSynthetic,
            node._2.id,
            node._2.name,
            node._2.owner,
            node._2.kind)
            .mkString(",")
        }.mkString("\n")
    )

    val nodesStr = nodesByID
      .map {case (id, node) => s"  $id // ${node.owner}.${node.name}"}
      .mkString("\n")

    val edgesStr = distinctEdges.map {edge =>
      val node1 = nodesByID(edge.id1)
      val node2 = nodesByID(edge.id2)
      s"  ${edge.id1} -> ${edge.id2} // ${node1.owner}.${node1.name} -> ${node2.owner}.${node2.name}"
    }.mkString("\n")

    val usedNodes = distinctEdges.map(_.id2).toSet

    val unusedNodes = nodesByID -- usedNodes

    val unsedNodesStr = unusedNodes
      .map {case (id, node) => s"  $id // ${node.owner}.${node.name}"}
      .mkString("\n")

    writeOutputFile(
      projectName,
      "unused-nodes",
      unsedNodesStr
    )

    writeOutputFile(
      projectName,
      s"$projectName.dot",
      s"digraph $projectName {\n$nodesStr\n$edgesStr\n}"
    )

    writeOutputFile(projectName, "edges",
      "id1,edgeKind,id2\n" +
        distinctEdges.map {edge =>
          val node1 = nodesByID(edge.id1)
          val node2 = nodesByID(edge.id2)
          List(edge.id1, edge.edgeKind, edge.id2).mkString(",") +
            s" // ${node1.owner}.${node1.name} -> ${node2.owner}.${node2.name}"
        }.mkString("\n"))
  }
}

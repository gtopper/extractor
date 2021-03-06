package com.topper.plugin

case class Edge(id1: Int,
                edgeKind: String,
                id2: Int)

case class Node(id: Int,
                name: String,
                owner: String,
                kind: String,
                synthetic: Boolean,
                fileName: Option[String]) {
  var ownersTraversed = false
}

case class Graph(nodes: Vector[Node], edges: Vector[Edge]) {

  def +(that: Graph): Graph = Graph(this.nodes ++ that.nodes, this.edges ++ that.edges)
}

package extractor.plugin

import scala.tools.nsc.Global

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

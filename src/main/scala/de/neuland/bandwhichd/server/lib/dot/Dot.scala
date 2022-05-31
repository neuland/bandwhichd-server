package de.neuland.bandwhichd.server.lib.dot

sealed trait Dot

case class Graph(`type`: GraphType, statements: Seq[Statement]) extends Dot

sealed trait GraphType
case object Undirected extends GraphType
case object Directed extends GraphType

sealed trait Statement
case class Node(id: NodeId, attributes: Seq[Attribute]) extends Statement
case class Edge(idA: NodeId, idB: NodeId) extends Statement

case class NodeId(value: String)

sealed trait Attribute
object Attribute {
  case class Label(value: String) extends Attribute
}

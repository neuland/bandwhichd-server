package de.neuland.bandwhichd.server.lib.dot.fs2

import _root_.fs2.Chunk
import cats.implicits.*
import de.neuland.bandwhichd.server.lib.dot.*

trait BC[A] {
  def toByteChunk(a: A): Chunk[Byte]
  extension (a: A) {
    def bc: Chunk[Byte] = toByteChunk(a)
  }
}

extension [A](seq: Seq[A]) {
  def intersperse(element: A): Seq[A] =
    if (seq.size < 2) {
      seq
    } else {
      seq.init.flatMap(a => Seq(a, element)).appended(seq.last)
    }
}

given stringBC: BC[String] with {
  override def toByteChunk(string: String): Chunk[Byte] =
    Chunk.array(string.getBytes(java.nio.charset.StandardCharsets.UTF_8))
}

given nodeId(using BC[String]): BC[NodeId] with {
  override def toByteChunk(nodeId: NodeId): Chunk[Byte] =
    s"\"${nodeId.value.replace("\"", "\\\"")}\"".bc
}

given attributeBC(using BC[String]): BC[Attribute] with {
  override def toByteChunk(attribute: Attribute): Chunk[Byte] =
    attribute match
      case Attribute.Label(value) => s"label=\"$value\"".bc
}

given attributeSeqBC(using BC[String], BC[Attribute]): BC[Seq[Attribute]] with {
  override def toByteChunk(attributes: Seq[Attribute]): Chunk[Byte] =
    attributes.map(_.bc).intersperse(" ".bc).reduce { case (a, b) => a ++ b }
}

given statementBC(using
    BC[String],
    BC[Seq[Attribute]]
): BC[(Statement, GraphType)] with {
  override def toByteChunk(
      statementAndGraphType: (Statement, GraphType)
  ): Chunk[Byte] =
    statementAndGraphType._1 match
      case Node(id, attributes) if attributes.isEmpty => id.bc
      case Node(id, attributes) => id.bc ++ " [".bc ++ attributes.bc ++ "]".bc
      case Edge(idA, idB) =>
        idA.bc ++ (statementAndGraphType._2 match
          case Undirected => " -- "
          case Directed   => " -> "
        ).bc ++ idB.bc
}

given statementSeqBC(using
    BC[String],
    BC[(Statement, GraphType)]
): BC[(Seq[Statement], GraphType)] with {
  override def toByteChunk(
      statementsAndGraphType: (Seq[Statement], GraphType)
  ): Chunk[Byte] =
    statementsAndGraphType._1.foldLeft(Chunk.empty[Byte]) {
      case (acc, statement) =>
        acc ++ "    ".bc ++ (
          statement,
          statementsAndGraphType._2
        ).bc ++ ";\n".bc
    }
}

given graphBC(using BC[String], BC[(Seq[Statement], GraphType)]): BC[Graph]
  with {
  override def toByteChunk(graph: Graph): Chunk[Byte] =
    (graph.`type` match
      case Undirected => "graph {\n"
      case Directed   => "digraph {\n"
    ).bc ++ (graph.statements, graph.`type`).bc ++ "}".bc
}

given dotBC(using graphBC: BC[Graph]): BC[Dot] with {
  override def toByteChunk(dot: Dot): Chunk[Byte] =
    dot match
      case graph @ Graph(_, _) => graphBC.toByteChunk(graph)
}

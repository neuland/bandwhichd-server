package de.neuland.bandwhichd.server.lib.http4s.dot

import de.neuland.bandwhichd.server.lib.dot.Dot
import org.http4s.headers.`Content-Type`
import org.http4s.{Charset, EntityEncoder, MediaType}

object DotHttp4s {
  val mediaType: MediaType =
    MediaType(
      mainType = "text",
      subType = "vnd.graphviz"
    )
  val contentType: `Content-Type` =
    `Content-Type`(
      mediaType = mediaType,
      charset = Charset.`UTF-8`
    )

  implicit def dotEntityEncoder[F[_]]: EntityEncoder[F, Dot] = {
    import de.neuland.bandwhichd.server.lib.dot.fs2.{BC, given}
    EntityEncoder
      .Pure[_root_.fs2.Chunk[Byte]]
      .contramap[Dot](summon[BC[Dot]].toByteChunk)
      .withContentType(contentType)
  }
}

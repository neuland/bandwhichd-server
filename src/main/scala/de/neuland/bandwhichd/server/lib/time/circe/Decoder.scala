package de.neuland.bandwhichd.server.lib.time.circe

import de.neuland.bandwhichd.server.lib.time.Interval
import io.circe.{DecodingFailure, HCursor}
import org.http4s.circe.DecodingFailures

import java.time.DateTimeException

object Decoder {
  val intervalDecoder: io.circe.Decoder[Interval] =
    (c: HCursor) =>
      c.as[String](io.circe.Decoder.decodeString)
        .flatMap(string =>
          Interval
            .parse(string)
            .toEither
            .swap
            .map(error => DecodingFailure(error.getMessage, c.history))
            .swap
        )
}

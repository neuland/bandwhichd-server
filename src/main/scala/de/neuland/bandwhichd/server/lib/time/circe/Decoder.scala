package de.neuland.bandwhichd.server.lib.time.circe

import de.neuland.bandwhichd.server.lib.time.ZonedInterval
import io.circe.{DecodingFailure, HCursor}
import org.http4s.circe.DecodingFailures

import java.time.DateTimeException

object Decoder {
  val zonedIntervalDecoder: io.circe.Decoder[ZonedInterval] =
    (c: HCursor) =>
      c.as[String](io.circe.Decoder.decodeString)
        .flatMap(string =>
          ZonedInterval
            .parse(string)
            .toEither
            .swap
            .map(error => DecodingFailure(error.getMessage, c.history))
            .swap
        )
}

package de.neuland.bandwhichd.server.lib.ip4s.circe

import com.comcast.ip4s.*
import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor}

object Ip4sDecoders {
  val cidrDecoder: Decoder[Cidr[IpAddress]] =
    (c: HCursor) =>
      c.as[String](Decoder.decodeString)
        .flatMap(string =>
          Cidr
            .fromString(string)
            .fold[Decoder.Result[Cidr[IpAddress]]](
              Left(DecodingFailure(s"invalid cidr $string", c.history))
            )(
              Right.apply
            )
        )

  val hostnameDecoder: Decoder[Hostname] =
    (c: HCursor) =>
      c.as[String](Decoder.decodeString)
        .flatMap(string =>
          Hostname
            .fromString(string)
            .fold[Decoder.Result[Hostname]](
              Left(DecodingFailure(s"invalid hostname $string", c.history))
            )(
              Right.apply
            )
        )

  val socketAddressIpAddressDecoder: Decoder[SocketAddress[IpAddress]] =
    (c: HCursor) =>
      c.as[String](Decoder.decodeString)
        .flatMap(string =>
          SocketAddress
            .fromStringIp(string)
            .fold[Result[SocketAddress[IpAddress]]](
              Left(
                DecodingFailure(s"invalid socket address $string", c.history)
              )
            )(
              Right.apply
            )
        )
}

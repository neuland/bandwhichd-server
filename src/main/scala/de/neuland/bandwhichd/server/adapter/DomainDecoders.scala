package de.neuland.bandwhichd.server.adapter

import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.Timing.{
  Timeframe,
  Timestamp
}
import de.neuland.bandwhichd.server.lib.time.Interval
import io.circe.{Decoder, DecodingFailure, HCursor}

import java.time.{Duration, ZonedDateTime}
import java.util.UUID

object DomainDecoders {
  val agentIdDecoder: Decoder[AgentId] =
    (c: HCursor) => c.as[UUID](Decoder.decodeUUID).map(AgentId.apply)

  val bytesCountDecoder: Decoder[BytesCount] =
    (c: HCursor) => c.as[BigInt](Decoder.decodeBigInt).map(BytesCount.apply)

  val interfaceNameDecoder: Decoder[InterfaceName] =
    (c: HCursor) => c.as[String](Decoder.decodeString).map(InterfaceName.apply)

  val machineIdDecoder: Decoder[MachineId] =
    (c: HCursor) => c.as[UUID](Decoder.decodeUUID).map(MachineId.apply)

  val processNameDecoder: Decoder[ProcessName] =
    (c: HCursor) => c.as[String](Decoder.decodeString).map(ProcessName.apply)

  val protocolDecoder: Decoder[Protocol] =
    (c: HCursor) =>
      c.as[String](Decoder.decodeString)
        .flatMap(string => {
          string match
            case "tcp" => Right(Protocol.Tcp)
            case "udp" => Right(Protocol.Udp)
            case _ =>
              Left(DecodingFailure(s"invalid protocol $string", c.history))
        })

  val timeframeDecoder: Decoder[Timeframe] =
    (c: HCursor) =>
      c.as[Interval](
        de.neuland.bandwhichd.server.lib.time.circe.Decoder.intervalDecoder
      ).map(Timeframe.apply)

  val timestampDecoder: Decoder[Timestamp] =
    (c: HCursor) =>
      c.as[ZonedDateTime](Decoder.decodeZonedDateTime).map(Timestamp.apply)
}

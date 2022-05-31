package de.neuland.bandwhichd.server.adapter.in.v1.message

import de.neuland.bandwhichd.server.adapter.DomainDecoders.agentIdDecoder
import de.neuland.bandwhichd.server.domain.measurement.{Measurement, Timing}
import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor}
import org.http4s.circe.DecodingFailures

sealed trait Message

object Message {
  case class MeasurementMessage(measurement: Measurement[Timing])
      extends Message

  val decoder: Decoder[Message] =
    (c: HCursor) =>
      for {
        `type` <- c.get[String]("type")
        message <- `type` match
          case "bandwhichd/measurement/network-configuration/v1" =>
            c.get[Measurement.NetworkConfiguration]("content")(
              de.neuland.bandwhichd.server.adapter.in.v1.message.DomainDecodersV1.measurementNetworkConfigurationDecoder
            ).map(MeasurementMessage.apply)
          case "bandwhichd/measurement/network-utilization/v1" =>
            c.get[Measurement.NetworkUtilization]("content")(
              de.neuland.bandwhichd.server.adapter.in.v1.message.DomainDecodersV1.measurementNetworkUtilizationDecoder
            ).map(MeasurementMessage.apply)
          case _ =>
            Left(
              DecodingFailure(s"invalid message type ${`type`}", c.history)
            )
      } yield message
}

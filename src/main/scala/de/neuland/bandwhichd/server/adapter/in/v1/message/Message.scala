package de.neuland.bandwhichd.server.adapter.in.v1.message

import com.comcast.ip4s.{Cidr, Host, Hostname, IpAddress, SocketAddress}
import de.neuland.bandwhichd.server.domain.measurement.Timing
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.Protocol.{Tcp, Udp}
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.lib.time.Interval
import io.circe.Decoder.Result
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, HCursor, Json}
import org.http4s.circe.DecodingFailures

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.UUID

sealed trait Message

object Message {
  case class MeasurementMessage(measurement: Measurement[Timing])
      extends Message

  def apply(measurement: Measurement[Timing]): Message =
    MeasurementMessage(measurement)

  given Encoder[Message] =
    (message: Message) =>
      message match
        case MeasurementMessage(nc: Measurement.NetworkConfiguration) =>
          Json.obj(
            "type" -> Json.fromString(
              "bandwhichd/measurement/agent-network-configuration/v1"
            ),
            "content" -> Encoder[Measurement.NetworkConfiguration].apply(nc)
          )
        case MeasurementMessage(nu: Measurement.NetworkUtilization) =>
          Json.obj(
            "type" -> Json.fromString(
              "bandwhichd/measurement/agent-network-utilization/v1"
            ),
            "content" -> Encoder[Measurement.NetworkUtilization].apply(nu)
          )

  given Decoder[Message] =
    (c: HCursor) =>
      for {
        `type` <- c.get[String]("type")
        message <- `type` match
          case "bandwhichd/measurement/agent-network-configuration/v1" =>
            c.get[Measurement.NetworkConfiguration]("content")
              .map(MeasurementMessage.apply)
          case "bandwhichd/measurement/agent-network-utilization/v1" =>
            c.get[Measurement.NetworkUtilization]("content")
              .map(MeasurementMessage.apply)
          case _ =>
            Left(DecodingFailure(s"invalid message type ${`type`}", c.history))
      } yield message

  given Codec[Measurement.NetworkConfiguration] =
    Codec.forProduct6(
      "machine_id",
      "timestamp",
      "maybe_os_release",
      "hostname",
      "interfaces",
      "open_sockets"
    )(Measurement.NetworkConfiguration.apply)(nc =>
      (
        nc.machineId,
        nc.timing,
        nc.maybeOsRelease,
        nc.hostname,
        nc.interfaces,
        nc.openSockets
      )
    )

  given Codec[Measurement.NetworkUtilization] =
    Codec.forProduct3(
      "machine_id",
      "timeframe",
      "connections"
    )(Measurement.NetworkUtilization.apply)(nu =>
      (
        nu.machineId,
        nu.timing,
        nu.connections
      )
    )

  given Codec[Interface] =
    Codec.forProduct3(
      "name",
      "is_up",
      "networks"
    )(Interface.apply)(interface =>
      (
        interface.name,
        interface.isUp,
        interface.networks
      )
    )

  given Codec[Connection] =
    Codec.forProduct6(
      "interface_name",
      "local_socket_address",
      "remote_socket_address",
      "protocol",
      "received",
      "sent"
    )(Connection.apply)(connection =>
      (
        connection.interfaceName,
        connection.localSocket,
        connection.remoteSocket,
        connection.protocol,
        connection.received,
        connection.sent
      )
    )

  given Codec[OpenSocket] =
    Codec.forProduct3(
      "socket_address",
      "protocol",
      "process"
    )(OpenSocket.apply)(openSocket =>
      (
        openSocket.socket,
        openSocket.protocol,
        openSocket.maybeProcessName
      )
    )

  ///////////////////////

  given localEncoder[A](using Encoder[A]): Encoder[Local[A]] =
    Encoder[A].contramap(_.value)
  given localDecoder[A](using Decoder[A]): Decoder[Local[A]] =
    Decoder[A].map(Local.apply)
  given remoteEncoder[A](using Encoder[A]): Encoder[Remote[A]] =
    Encoder[A].contramap(_.value)
  given remoteDecoder[A](using Decoder[A]): Decoder[Remote[A]] =
    Decoder[A].map(Remote.apply)
  given receivedEncoder[A](using Encoder[A]): Encoder[Received[A]] =
    Encoder[A].contramap(_.value)
  given receivedDecoder[A](using Decoder[A]): Decoder[Received[A]] =
    Decoder[A].map(Received.apply)
  given sentEncoder[A](using Encoder[A]): Encoder[Sent[A]] =
    Encoder[A].contramap(_.value)
  given sentDecoder[A](using Decoder[A]): Decoder[Sent[A]] =
    Decoder[A].map(Sent.apply)
  ///////////////////////

  given Encoder[BytesCount] = Encoder[String].contramap(_.value.toString)
  given Decoder[BytesCount] = Decoder[BigInt].map(BytesCount.apply)
  given Encoder[InterfaceName] = Encoder[String].contramap(_.value)
  given Decoder[InterfaceName] = Decoder[String].map(InterfaceName.apply)
  given Encoder[MachineId] = Encoder[UUID].contramap(_.value)
  given Decoder[MachineId] = Decoder[UUID].map(MachineId.apply)
  given Encoder[OsRelease.FileContents] = Encoder[String].contramap(_.value)
  given Decoder[OsRelease.FileContents] =
    Decoder[String].map(OsRelease.FileContents.apply)
  given Encoder[ProcessName] = Encoder[String].contramap(_.value)
  given Decoder[ProcessName] = Decoder[String].map(ProcessName.apply)
  given Encoder[Protocol] = Encoder[String].contramap(_ match
    case Protocol.Tcp => "tcp"
    case Protocol.Udp => "udp"
  )
  given Decoder[Protocol] = Decoder[String].emap(value =>
    value match
      case "tcp" => Right(Protocol.Tcp)
      case "udp" => Right(Protocol.Udp)
      case _     => Left(s"invalid protocol $value")
  )

  ///////////////////////

  given Encoder[Timing.Timestamp] =
    Encoder[ZonedDateTime].contramap(_.instant.atZone(UTC))
  given Decoder[Timing.Timestamp] =
    Decoder[ZonedDateTime].map(Timing.Timestamp.apply)

  given Encoder[Timing.Timeframe] = Encoder[Interval].contramap(_.interval)
  given Decoder[Timing.Timeframe] =
    Decoder[Interval].map(Timing.Timeframe.apply)

  given Encoder[Interval] = Encoder[String].contramap(_.toString)
  given Decoder[Interval] = Decoder[String].emap(value =>
    Interval
      .parse(value)
      .toEither
      .swap
      .map(_.getMessage)
      .swap
  )

  ///////////////////////

  given Encoder[Cidr[IpAddress]] =
    Encoder[String].contramap(_.toString)
  given Decoder[Cidr[IpAddress]] = Decoder[String].emap(value =>
    Cidr.fromString(value).toRight(s"invalid cidr $value")
  )

  given Encoder[Hostname] = Encoder[String].contramap(_.toString)
  given Decoder[Hostname] = Decoder[String].emap(value =>
    Hostname.fromString(value).toRight(s"invalid hostname $value")
  )

  given Encoder[SocketAddress[Host]] =
    Encoder[String].contramap(_.toString)
  given Decoder[SocketAddress[Host]] = Decoder[String].emap(value =>
    SocketAddress.fromStringIp(value).toRight(s"invalid socket address $value")
  )
}

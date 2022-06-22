package de.neuland.bandwhichd.server.adapter.out.measurement

import com.comcast.ip4s.{Cidr, Host, Hostname, IpAddress, SocketAddress}
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.lib.time.Interval
import io.circe.*

import java.time.ZoneOffset.UTC
import java.time.{Instant, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.util.UUID
import scala.util.Try

object MeasurementCassandraCodecs {
  given Encoder[Measurement[Timing]] =
    (measurement: Measurement[Timing]) =>
      measurement match
        case nc: Measurement.NetworkConfiguration =>
          Encoder[Measurement.NetworkConfiguration].mapJson(
            _.mapObject(
              _.add(
                "measurement_type",
                Json.fromString("network_configuration")
              )
            )
          )(nc)
        case nu: Measurement.NetworkUtilization =>
          Encoder[Measurement.NetworkUtilization].mapJson(
            _.mapObject(
              _.add(
                "measurement_type",
                Json.fromString("network_utilization")
              )
            )
          )(nu)

  given Decoder[Measurement[Timing]] =
    (c: HCursor) => {
      val measurementTypeCursor = c.downField("measurement_type")
      for {
        measurementType <- measurementTypeCursor.as[String]
        measurement <- {
          measurementType match
            case "network_configuration" =>
              c.as[Measurement.NetworkConfiguration]
            case "network_utilization" =>
              c.as[Measurement.NetworkUtilization]
            case _ =>
              Left(
                DecodingFailure(
                  s"invalid measurement type $measurementType",
                  measurementTypeCursor.history
                )
              )
        }
      } yield measurement
    }

  given Codec[Measurement.NetworkConfiguration] =
    Codec.forProduct6(
      "agent_id",
      "timestamp",
      "network_configuration_machine_id",
      "network_configuration_hostname",
      "network_configuration_interfaces",
      "network_configuration_open_sockets"
    )(
      (
          agentId: AgentId,
          timestamp: Timing.Timestamp,
          machinedId: MachineId,
          hostname: Hostname,
          interfaces: Seq[Interface],
          openSockets: Seq[OpenSocket]
      ) =>
        Measurement.NetworkConfiguration(
          agentId = agentId,
          timing = timestamp,
          machineId = machinedId,
          hostname = hostname,
          interfaces = interfaces,
          openSockets = openSockets
        )
    )(nc =>
      (
        nc.agentId,
        nc.timing,
        nc.machineId,
        nc.hostname,
        nc.interfaces,
        nc.openSockets
      )
    )

  given Codec[Interface] =
    Codec.forProduct3("name", "is_up", "networks")(Interface.apply)(interface =>
      (interface.name, interface.isUp, interface.networks)
    )

  given Codec[OpenSocket] =
    Codec.forProduct3("socket", "protocol", "maybe_process_name")(
      OpenSocket.apply
    )(openSocket =>
      (openSocket.socket, openSocket.protocol, openSocket.maybeProcessName)
    )

  given Codec[Measurement.NetworkUtilization] =
    Codec.forProduct4(
      "agent_id",
      "timestamp",
      "end_timestamp",
      "network_utilization_connections"
    )(
      (
          agentId: AgentId,
          timestamp: Timing.Timestamp,
          endTimestamp: Timing.Timestamp,
          connections: Seq[Connection]
      ) =>
        Measurement.NetworkUtilization(
          agentId = agentId,
          timing = Timing.Timeframe(
            Interval(
              start = timestamp.instant,
              stop = endTimestamp.instant
            )
          ),
          connections = connections
        )
    )(nu =>
      (
        nu.agentId,
        Timing.Timestamp(nu.timing.value.normalizedStart),
        Timing.Timestamp(nu.timing.value.normalizedStop),
        nu.connections
      )
    )

  given Codec[Connection] =
    Codec.forProduct6(
      "interface_name",
      "local_socket",
      "remote_socket",
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

  given Encoder[AgentId] = Encoder[UUID].contramap(_.value)
  given Decoder[AgentId] = Decoder[UUID].map(AgentId.apply)
  given Encoder[BytesCount] = Encoder[BigInt].contramap(_.value)
  given Decoder[BytesCount] = Decoder[BigInt].map(BytesCount.apply)
  given Encoder[InterfaceName] = Encoder[String].contramap(_.value)
  given Decoder[InterfaceName] = Decoder[String].map(InterfaceName.apply)
  given Encoder[MachineId] = Encoder[UUID].contramap(_.value)
  given Decoder[MachineId] = Decoder[UUID].map(MachineId.apply)
  given Encoder[ProcessName] = Encoder[String].contramap(_.value)
  given Decoder[ProcessName] = Decoder[String].map(ProcessName.apply)
  given Encoder[Protocol] =
    Encoder[String].contramap(_ match
      case Protocol.Tcp => "tcp"
      case Protocol.Udp => "udp"
    )
  given Decoder[Protocol] =
    Decoder[String].emap(value =>
      value match
        case "tcp" => Right(Protocol.Tcp)
        case "udp" => Right(Protocol.Udp)
        case _     => Left(s"invalid protocol $value")
    )

  ///////////////////////

  val cassandraTimestampJsonFormatter: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .appendLiteral(' ')
      .append(DateTimeFormatter.ISO_LOCAL_TIME)
      .appendOffsetId()
      .toFormatter
  val cassandraTimestampJsonEncoder: Encoder[OffsetDateTime] =
    Encoder.encodeOffsetDateTimeWithFormatter(cassandraTimestampJsonFormatter)
  val cassandraTimestampJsonDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeOffsetDateTimeWithFormatter(cassandraTimestampJsonFormatter)

  given Encoder[Timing.Timestamp] =
    cassandraTimestampJsonEncoder.contramap(_.instant.atOffset(UTC))
  given Decoder[Timing.Timestamp] =
    cassandraTimestampJsonDecoder.map(value =>
      Timing.Timestamp(value.toInstant)
    )

  ///////////////////////

  given Encoder[Hostname] = Encoder[String].contramap(_.toString)
  given Decoder[Hostname] = Decoder[String].emap(value =>
    Hostname.fromString(value).toRight(s"invalid hostname $value")
  )
  given Encoder[SocketAddress[Host]] = Encoder[String].contramap(_.toString)
  given Decoder[SocketAddress[Host]] = Decoder[String].emap(value =>
    SocketAddress.fromString(value).toRight(s"invalid socket address $value")
  )
  given Codec[Cidr[IpAddress]] =
    Codec.forProduct2("address", "prefix_bits")(Cidr.apply)(cidr =>
      (cidr.address, cidr.prefixBits)
    )
  given Encoder[IpAddress] = Encoder[String].contramap(_.toString)
  given Decoder[IpAddress] = Decoder[String].emap(value =>
    IpAddress.fromString(value).toRight(s"invalid ip address $value")
  )
}

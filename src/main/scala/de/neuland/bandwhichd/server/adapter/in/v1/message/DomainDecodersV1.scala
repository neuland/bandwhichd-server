package de.neuland.bandwhichd.server.adapter.in.v1.message

import com.comcast.ip4s.*
import de.neuland.bandwhichd.server.adapter.DomainDecoders
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.lib.ip4s.circe.Ip4sDecoders
import io.circe.{Decoder, HCursor}

object DomainDecodersV1 {
  val measurementNetworkConfigurationDecoder
      : Decoder[Measurement.NetworkConfiguration] =
    (c: HCursor) =>
      for {
        agentId <- c.get[AgentId]("agent_id")(
          DomainDecoders.agentIdDecoder
        )
        timestamp <- c.get[Timing.Timestamp]("timestamp")(
          DomainDecoders.timestampDecoder
        )
        machineId <- c.get[MachineId]("machine_id")(
          DomainDecoders.machineIdDecoder
        )
        hostname <- c.get[Hostname]("hostname")(Ip4sDecoders.hostnameDecoder)
        interfaces <- c.get[Seq[Interface]]("interfaces")(
          Decoder.decodeSeq(interfaceDecoder)
        )
        openSockets <- c.get[Seq[OpenSocket]]("open_sockets")(
          Decoder.decodeSeq(openSocketDecoder)
        )
      } yield Measurement.NetworkConfiguration(
        agentId = agentId,
        timing = timestamp,
        machineId = machineId,
        hostname = hostname,
        interfaces = interfaces,
        openSockets = openSockets
      )

  val measurementNetworkUtilizationDecoder
      : Decoder[Measurement.NetworkUtilization] =
    (c: HCursor) =>
      for {
        agentId <- c.get[AgentId]("agent_id")(
          DomainDecoders.agentIdDecoder
        )
        timeframe <- c.get[Timing.Timeframe]("timeframe")(
          DomainDecoders.timeframeDecoder
        )
        connections <- c.get[Seq[Connection]]("connections")(
          Decoder.decodeSeq(connectionDecoder)
        )
      } yield Measurement.NetworkUtilization(
        agentId = agentId,
        timing = timeframe,
        connections = connections
      )

  val interfaceDecoder: Decoder[Interface] =
    (c: HCursor) =>
      for {
        name <- c.get[InterfaceName]("name")(
          DomainDecoders.interfaceNameDecoder
        )
        isUp <- c.get[Boolean]("is_up")(Decoder.decodeBoolean)
        networks <- c.get[Seq[Cidr[IpAddress]]]("networks")(
          Decoder.decodeSeq(Ip4sDecoders.cidrDecoder)
        )
      } yield Interface(
        name = name,
        isUp = isUp,
        networks = networks
      )

  val connectionDecoder: Decoder[Connection] =
    (c: HCursor) =>
      for {
        interfaceName <- c.get[InterfaceName]("interface_name")(
          DomainDecoders.interfaceNameDecoder
        )
        localSocketAddress <- c.get[SocketAddress[IpAddress]](
          "local_socket_address"
        )(
          Ip4sDecoders.socketAddressIpAddressDecoder
        )
        remoteSocketAddress <- c.get[SocketAddress[IpAddress]](
          "remote_socket_address"
        )(
          Ip4sDecoders.socketAddressIpAddressDecoder
        )
        protocol <- c.get[Protocol]("protocol")(
          DomainDecoders.protocolDecoder
        )
        received <- c.get[BytesCount]("received")(
          DomainDecoders.bytesCountDecoder
        )
        sent <- c.get[BytesCount]("sent")(
          DomainDecoders.bytesCountDecoder
        )
      } yield Connection(
        interfaceName = interfaceName,
        localSocket = Local(localSocketAddress),
        remoteSocket = Remote(remoteSocketAddress),
        protocol = protocol,
        received = Received(received),
        sent = Sent(sent)
      )

  val openSocketDecoder: Decoder[OpenSocket] =
    (c: HCursor) =>
      for {
        socketAddress <- c.get[SocketAddress[IpAddress]]("socket_address")(
          Ip4sDecoders.socketAddressIpAddressDecoder
        )
        protocol <- c.get[Protocol]("protocol")(DomainDecoders.protocolDecoder)
        maybeProcessName <- c.get[Option[ProcessName]]("process")(
          Decoder.decodeOption(DomainDecoders.processNameDecoder)
        )
      } yield OpenSocket(
        socket = socketAddress,
        protocol = protocol,
        maybeProcessName = maybeProcessName
      )
}

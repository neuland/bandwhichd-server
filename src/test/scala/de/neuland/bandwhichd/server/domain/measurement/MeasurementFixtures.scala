package de.neuland.bandwhichd.server.domain.measurement

import cats.data.NonEmptySeq
import com.comcast.ip4s.*
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.lib.time.Interval

import java.time.{Duration, Instant, ZonedDateTime}
import java.util.UUID

object MeasurementFixtures {
  val exampleNetworkConfigurationMeasurement: Measurement.NetworkConfiguration =
    Measurement.NetworkConfiguration(
      agentId =
        AgentId(UUID.fromString("d254aebd-e092-4ced-b698-0448a46eaf7d")),
      timing =
        Timing.Timestamp(ZonedDateTime.parse("2022-05-06T15:14:51.74223728Z")),
      machineId =
        MachineId(UUID.fromString("c414c2da-714c-4b68-b97e-3f31e18053d2")),
      hostname = Hostname.fromString("some-host.example.com").get,
      interfaces = Seq(
        Interface(
          name = InterfaceName("docker0"),
          isUp = false,
          networks = Seq(
            Cidr(
              address = Ipv4Address.fromBytes(172, 17, 0, 1),
              prefixBits = 16
            ),
            Cidr(
              address = Ipv6Address.fromString("fe80::42:a4ff:fef2:4ad4").get,
              prefixBits = 64
            )
          )
        ),
        Interface(
          name = InterfaceName("enp0s31f6"),
          isUp = false,
          networks = Seq.empty
        ),
        Interface(
          name = InterfaceName("lo"),
          isUp = true,
          networks = Seq(
            Cidr(
              address = Ipv4Address.fromBytes(127, 0, 0, 1),
              prefixBits = 8
            ),
            Cidr(
              address = Ipv6Address.fromString("::1").get,
              prefixBits = 128
            )
          )
        ),
        Interface(
          name = InterfaceName("virbr0"),
          isUp = false,
          networks = Seq(
            Cidr(
              address = Ipv4Address.fromBytes(192, 168, 122, 1),
              prefixBits = 24
            )
          )
        ),
        Interface(
          name = InterfaceName("tun0"),
          isUp = false,
          networks = Seq(
            Cidr(
              address = Ipv4Address.fromBytes(192, 168, 10, 87),
              prefixBits = 24
            )
          )
        ),
        Interface(
          name = InterfaceName("wlp3s0"),
          isUp = true,
          networks = Seq(
            Cidr(
              address = Ipv4Address.fromBytes(172, 18, 195, 209),
              prefixBits = 16
            ),
            Cidr(
              address = Ipv6Address.fromString("fe80::8e71:453d:204d:abf8").get,
              prefixBits = 64
            )
          )
        )
      ),
      openSockets = Seq(
        OpenSocket(
          socket = SocketAddress(
            Ipv4Address.fromBytes(0, 0, 0, 0),
            Port.fromInt(68).get
          ),
          protocol = Protocol.Udp,
          maybeProcessName = Some(ProcessName("dhclient"))
        ),
        OpenSocket(
          socket = SocketAddress(
            Ipv6Address.fromString("::").get,
            Port.fromInt(37863).get
          ),
          protocol = Protocol.Tcp,
          maybeProcessName = None
        )
      )
    )

  val exampleNetworkUtilizationMeasurement: Measurement.NetworkUtilization =
    Measurement.NetworkUtilization(
      agentId =
        AgentId(UUID.fromString("d254aebd-e092-4ced-b698-0448a46eaf7d")),
      timing = Timing.Timeframe(
        Interval(
          start = Instant.parse("2022-05-06T15:14:51.74223728Z"),
          duration = Duration.parse("PT10.000148S")
        )
      ),
      connections = Seq(
        Connection(
          interfaceName = InterfaceName("lo"),
          localSocket = Local(
            SocketAddress(
              Ipv4Address.fromBytes(127, 0, 0, 1),
              Port.fromInt(8080).get
            )
          ),
          remoteSocket = Remote(
            SocketAddress(
              Ipv4Address.fromBytes(127, 0, 0, 1),
              Port.fromInt(36070).get
            )
          ),
          protocol = Protocol.Tcp,
          received = Received(BytesCount(BigInt(608))),
          sent = Sent(BytesCount(BigInt(0)))
        ),
        Connection(
          interfaceName = InterfaceName("lo"),
          localSocket = Local(
            SocketAddress(
              Ipv4Address.fromBytes(127, 0, 0, 1),
              Port.fromInt(36070).get
            )
          ),
          remoteSocket = Remote(
            SocketAddress(
              Ipv4Address.fromBytes(127, 0, 0, 1),
              Port.fromInt(8080).get
            )
          ),
          protocol = Protocol.Tcp,
          received = Received(BytesCount(BigInt(0))),
          sent = Sent(BytesCount(BigInt(13882)))
        ),
        Connection(
          interfaceName = InterfaceName("tun0"),
          localSocket = Local(
            SocketAddress(
              Ipv4Address.fromBytes(192, 168, 10, 87),
              Port.fromInt(43254).get
            )
          ),
          remoteSocket = Remote(
            SocketAddress(
              Ipv4Address.fromBytes(192, 168, 10, 34),
              Port.fromInt(5353).get
            )
          ),
          protocol = Protocol.Udp,
          received = Received(BytesCount(BigInt(120))),
          sent = Sent(BytesCount(BigInt(64)))
        ),
        Connection(
          interfaceName = InterfaceName("wlp3s0"),
          localSocket = Local(
            SocketAddress(
              Ipv4Address.fromBytes(172, 18, 195, 209),
              Port.fromInt(41234).get
            )
          ),
          remoteSocket = Remote(
            SocketAddress(
              Ipv4Address.fromBytes(45, 1, 2, 3),
              Port.fromInt(80).get
            )
          ),
          protocol = Protocol.Tcp,
          received = Received(BytesCount(BigInt(653808))),
          sent = Sent(BytesCount(BigInt(1365)))
        )
      )
    )

  val allTimestamps: Seq[Timing.Timestamp] =
    Seq(
      exampleNetworkConfigurationMeasurement.timing,
      exampleNetworkUtilizationMeasurement.timing.start,
      exampleNetworkUtilizationMeasurement.timing.end
    )
  val fullTimeframe: Timing.Timeframe =
    Timing.Timeframe.encompassing(NonEmptySeq.fromSeqUnsafe(allTimestamps))
}

package de.neuland.bandwhichd.server.domain.measurement

import cats.data.NonEmptySeq
import com.comcast.ip4s.*
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.stats.HostId
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.test.Arbitraries.{sample, given}

import java.time.{Duration, Instant, ZonedDateTime}
import java.util.UUID

object MeasurementFixtures {
  val exampleNetworkConfigurationMeasurement: Measurement.NetworkConfiguration =
    Measurement.NetworkConfiguration(
      machineId =
        MachineId(UUID.fromString("c414c2da-714c-4b68-b97e-3f31e18053d2")),
      timing =
        Timing.Timestamp(ZonedDateTime.parse("2022-05-06T15:14:51.742Z")),
      maybeOsRelease = Some(
        OsRelease.FileContents(
          "PRETTY_NAME=\"Debian GNU/Linux 11 (bullseye)\"\nNAME=\"Debian GNU/Linux\"\nVERSION_ID=\"11\"\nVERSION=\"11 (bullseye)\"\nVERSION_CODENAME=bullseye\nID=debian\nHOME_URL=\"https://www.debian.org/\"\nSUPPORT_URL=\"https://www.debian.org/support\"\nBUG_REPORT_URL=\"https://bugs.debian.org/\""
        )
      ),
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
      machineId =
        MachineId(UUID.fromString("c414c2da-714c-4b68-b97e-3f31e18053d2")),
      timing = Timing.Timeframe(
        Interval(
          start = Instant.parse("2022-05-06T15:14:51.942Z"),
          duration = Duration.parse("PT10.001S")
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
    Timing.Timeframe(NonEmptySeq.fromSeqUnsafe(allTimestamps))

  case class TimeframeDataset(
      host1: Host,
      host2: Host,
      host3: Host,
      con1: Connection,
      con2: Connection,
      con3: Connection,
      nc: Measurement.NetworkConfiguration,
      nuBefore: Measurement.NetworkUtilization,
      nuAfter: Measurement.NetworkUtilization,
      cutoffTimestamp: Timing.Timestamp
  ) {
    def hostId0: HostId = HostId(nc.machineId)
    def hostId1: HostId = HostId(con1.remoteSocket.value.host)
    def hostId2: HostId = HostId(con2.remoteSocket.value.host)
    def hostId3: HostId = HostId(con3.remoteSocket.value.host)

    def measurements: Seq[Measurement[Timing]] =
      Seq(nc, nuBefore, nuAfter)
  }

  object TimeframeDataset {
    def gen(): TimeframeDataset = {
      val host0 = ipv4"192.168.0.10"
      val host1 = ipv4"192.168.0.11"
      val host2 = ipv4"192.168.0.12"
      val host3 = ipv4"192.168.0.13"

      val nc = MeasurementFixtures
        .ncGen()
        .copy(
          hostname = host"host0",
          maybeOsRelease = None,
          interfaces = Seq(
            Interface(
              name = InterfaceName("eth0"),
              isUp = true,
              networks = Seq(Cidr(host0, 24))
            )
          ),
          openSockets = Seq.empty
        )
      val baseTiming = nc.timestamp.instant

      val nuTemplate =
        MeasurementFixtures.nuGen().copy(machineId = nc.machineId)

      val con1 = MeasurementFixtures
        .conGen()
        .copy(
          localSocket = Local(SocketAddress(host0, port"50101")),
          remoteSocket = Remote(SocketAddress(host1, port"8080"))
        )
      val con2 = MeasurementFixtures
        .conGen()
        .copy(
          localSocket = Local(SocketAddress(host0, port"50102")),
          remoteSocket = Remote(SocketAddress(host2, port"8080"))
        )
      val con3 = MeasurementFixtures
        .conGen()
        .copy(
          localSocket = Local(SocketAddress(host0, port"50103")),
          remoteSocket = Remote(SocketAddress(host3, port"8080"))
        )

      val nuBefore = nuTemplate.copy(
        timing = Timing.Timeframe(
          Interval(baseTiming.plusMillis(4), nuTemplate.timing.duration)
        ),
        connections = Seq(con1, con2)
      )
      val nuAfter = nuTemplate.copy(
        timing = Timing.Timeframe(
          Interval(baseTiming.plusSeconds(10), nuTemplate.timing.duration)
        ),
        connections = Seq(con2, con3)
      )

      val cutoffTimestamp =
        Timing.Timestamp(baseTiming.plusSeconds(15))

      TimeframeDataset(
        host1 = host1,
        host2 = host2,
        host3 = host3,
        con1 = con1,
        con2 = con2,
        con3 = con3,
        nc = nc,
        nuBefore = nuBefore,
        nuAfter = nuAfter,
        cutoffTimestamp = cutoffTimestamp
      )
    }
  }

  def ncGen: () => Measurement.NetworkConfiguration =
    sample[Measurement.NetworkConfiguration]
  def nuGen: () => Measurement.NetworkUtilization =
    sample[Measurement.NetworkUtilization]
  def conGen: () => Connection =
    sample[Connection]
}

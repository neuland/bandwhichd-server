package de.neuland.bandwhichd.server.domain.stats

import fs2.Stream
import cats.effect.IO
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.comcast.ip4s.{Cidr, Host, Hostname, Ipv4Address, Port, SocketAddress}
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AsyncWordSpec}

import java.time.{Duration, Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

class StatsSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {
  "Stats" when {
    "built from a network configuration" should {

      // given
      val measurements: Seq[Measurement[Timing]] = Seq(
        Measurement.NetworkConfiguration(
          agentId =
            AgentId(UUID.fromString("0b7f7d58-3c5c-4f92-9ada-4c27ab19b195")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          machineId =
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
      )

      val measurementsStream: Stream[IO, Measurement[Timing]] =
        Stream.emits(measurements)

      // when
      val resultF: IO[Stats] = Stats[IO](measurementsStream)

      "have a host id" in {
        // then
        resultF.asserting { result =>
          result.hosts should have size 1
          result.hosts.head.hostId shouldBe HostId(
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228"))
          )
        }
      }

      "have the hostname" in {
        // then
        resultF.asserting { result =>
          result.hosts should have size 1
          result.hosts.head.hostname shouldBe Hostname
            .fromString("some-host.example.com")
            .get
        }
      }
    }

    "built from a network configuration with hostname collision according to the machine ids" should {

      // given
      val measurements: Seq[Measurement[Timing]] = Seq(
        Measurement.NetworkConfiguration(
          agentId =
            AgentId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          machineId =
            MachineId(UUID.fromString("0b7f7d58-3c5c-4f92-9ada-4c27ab19b195")),
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        ),
        Measurement.NetworkConfiguration(
          agentId =
            AgentId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          machineId =
            MachineId(UUID.fromString("0c8cb005-a78b-49c5-8760-48daf08ea86f")),
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
      )

      val measurementsStream: Stream[IO, Measurement[Timing]] =
        Stream.emits(measurements)

      // when
      val resultF: IO[Stats] = Stats[IO](measurementsStream)

      "not merge hosts with the same hostname" in {
        // then
        resultF.asserting { result =>
          result.hosts.foreach(host =>
            host.hostname shouldBe Hostname
              .fromString("some-host.example.com")
              .get
          )
          result.hosts should have size 2
        }
      }
    }

    "built from a network configuration with multiple hostnames for the same host according to the machine ids" should {

      // given
      val measurements: Seq[Measurement[Timing]] = Seq(
        Measurement.NetworkConfiguration(
          agentId =
            AgentId(UUID.fromString("0b7f7d58-3c5c-4f92-9ada-4c27ab19b195")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:10:50.34957395Z")
          ),
          machineId =
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          hostname = Hostname.fromString("another-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        ),
        Measurement.NetworkConfiguration(
          agentId =
            AgentId(UUID.fromString("0b7f7d58-3c5c-4f92-9ada-4c27ab19b195")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          machineId =
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
      )

      val measurementsStream: Stream[IO, Measurement[Timing]] =
        Stream.emits(measurements)

      // when
      val resultF: IO[Stats] = Stats[IO](measurementsStream)

      "merge hosts with the same agent id" in {
        // then
        resultF.asserting { result =>
          result.hosts should have size 1
          result.hosts.head.hostId shouldBe HostId(
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228"))
          )
        }
      }

      "have primary hostname from most recent data and keep track of other hostnames" in {
        // then
        resultF.asserting { result =>
          result.hosts should have size 1
          result.hosts.head.hostname shouldBe Hostname
            .fromString("some-host.example.com")
            .get
          result.hosts.head.additionalHostnames should have size 1
          result.hosts.head.additionalHostnames should contain(
            Hostname.fromString("another-host.example.com").get
          )
        }
      }
    }

    "built from a network configuration and network utilization" should {

      // given
      val agentId1 =
        AgentId(UUID.fromString("4f705b72-8889-4dfd-9720-9b8a8d0f8f24"))
      val machineId1 =
        MachineId(UUID.fromString("8cf801f1-5592-49ce-a5e3-256648dfd7ea"))
      val hostId1 = HostId(machineId1)
      val hostname1 = Hostname.fromString("some-host.example.com").get
      val agentId2 =
        AgentId(UUID.fromString("c826b822-69b4-4dcc-844f-e1c604a8b724"))
      val machineId2 =
        MachineId(UUID.fromString("a54adaa9-2e35-4881-bc87-4e53d29d68ff"))
      val hostId2 = HostId(machineId2)
      val hostname2 = Hostname.fromString("another-host.example.com").get
      val start1 =
        Timing.Timestamp(ZonedDateTime.parse("2022-05-20T13:23:22.27689893Z"))
      val start2 =
        Timing.Timestamp(ZonedDateTime.parse("2022-05-20T13:23:24.91234515Z"))
      val defaultMeasuringInterval = Duration.ofSeconds(10)
      val timeframe1a =
        Timing.Timeframe(
          start = start1,
          duration = defaultMeasuringInterval
        )
      val timeframe1b = Timing.Timeframe(
        start = timeframe1a.end,
        duration = defaultMeasuringInterval
      )
      val timeframe2a =
        Timing.Timeframe(
          start = start2,
          duration = defaultMeasuringInterval
        )
      val timeframe2b = Timing.Timeframe(
        start = timeframe2a.end,
        duration = defaultMeasuringInterval
      )

      val measurements: Seq[Measurement[Timing]] = Seq(
        Measurement.NetworkConfiguration(
          agentId = agentId1,
          timing = start1,
          machineId = machineId1,
          hostname = hostname1,
          interfaces = Seq(
            Interface(
              name = InterfaceName("enp0s31f6"),
              isUp = true,
              networks = Seq(
                Cidr.fromString("192.168.0.10/24").get
              )
            )
          ),
          openSockets = Seq(
            OpenSocket(
              socket = SocketAddress.fromString("0.0.0.0:8080").get,
              protocol = Protocol.Tcp,
              maybeProcessName = Some(ProcessName("java"))
            )
          )
        ),
        Measurement.NetworkConfiguration(
          agentId = agentId2,
          timing = start2,
          machineId = machineId2,
          hostname = hostname2,
          interfaces = Seq(
            Interface(
              name = InterfaceName("enp0s31f6"),
              isUp = true,
              networks = Seq(
                Cidr.fromString("192.168.0.14/24").get
              )
            )
          ),
          openSockets = Seq.empty
        ),
        Measurement.NetworkUtilization(
          agentId = agentId1,
          timing = timeframe1a,
          connections = Seq(
            Connection(
              interfaceName = InterfaceName("enp0s31f6"),
              localSocket =
                Local(SocketAddress.fromString("192.168.0.10:8080").get),
              remoteSocket =
                Remote(SocketAddress.fromString("192.168.0.14:43254").get),
              protocol = Protocol.Tcp,
              received = Received(BytesCount(BigInt(5430))),
              sent = Sent(BytesCount(BigInt(32452)))
            )
          )
        ),
        Measurement.NetworkUtilization(
          agentId = agentId1,
          timing = timeframe1b,
          connections = Seq(
            Connection(
              interfaceName = InterfaceName("enp0s31f6"),
              localSocket =
                Local(SocketAddress.fromString("192.168.0.10:8080").get),
              remoteSocket =
                Remote(SocketAddress.fromString("192.168.0.14:43254").get),
              protocol = Protocol.Tcp,
              received = Received(BytesCount(BigInt(1000))),
              sent = Sent(BytesCount(BigInt(10000)))
            ),
            Connection(
              interfaceName = InterfaceName("enp0s31f6"),
              localSocket =
                Local(SocketAddress.fromString("192.168.0.10:34524").get),
              remoteSocket =
                Remote(SocketAddress.fromString("10.20.87.210:3000").get),
              protocol = Protocol.Tcp,
              received = Received(BytesCount(BigInt(65217))),
              sent = Sent(BytesCount(BigInt(3444)))
            )
          )
        ),
        Measurement.NetworkUtilization(
          agentId = agentId2,
          timing = timeframe2a,
          connections = Seq.empty
        ),
        Measurement.NetworkUtilization(
          agentId = agentId2,
          timing = timeframe2b,
          connections = Seq.empty
        )
      )

      val measurementsStream: Stream[IO, Measurement[Timing]] =
        Stream.emits(measurements)

      // when
      val resultF: IO[Stats] = Stats[IO](measurementsStream)

      def expectConnection(
          hostIds: (HostId, HostId)
      )(
          result: Stats
      ): (MonitoredHost, AnyHost) = {
        result.findConnection(hostIds) match
          case Some(connection) => connection
          case None =>
            val connectionHostIds = result.connections.map(connection =>
              connection._1.hostId -> connection._2.hostId
            )
            fail(
              s"expected to find connection between $hostIds in $connectionHostIds"
            )
      }

      "have all hosts" in {
        // then
        resultF.asserting { result =>
          result.hosts should have size 2
          result.hosts.map(_.hostId) should contain allOf (hostId1, hostId2)
        }
      }

      "have all connections" in {
        // then
        resultF.asserting { result =>
          result.connections should have size 2

          expectConnection(hostId1, hostId2)(result)

          val externalHost = Host.fromString("10.20.87.210").get
          val externalHostId = HostId(externalHost)
          val externalConnection =
            expectConnection(hostId1, externalHostId)(result)

          externalConnection._2 match
            case unidentifiedHost @ UnidentifiedHost(host) =>
              host shouldBe externalHost
              unidentifiedHost.hostId shouldBe externalHostId
            case _ => fail("expected unidentified host")
        }
      }
    }
  }
}

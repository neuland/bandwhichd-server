package de.neuland.bandwhichd.server.domain.stats

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.comcast.ip4s.*
import com.comcast.ip4s.Arbitraries.given
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.test.Arbitraries.{sample, given}
import fs2.Stream
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues
import org.scalatest.wordspec.{AnyWordSpec, AsyncWordSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.time.{Duration, Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

class StatsSpec
    extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues {
  "Stats" when {
    "empty" should {
      "append configuration" in {
        // given
        forAll { (nc: Measurement.NetworkConfiguration) =>

          // when
          val result = buildStats(nc)

          // then
          result.hosts should contain(
            MonitoredHost(
              hostId = HostId(nc.machineId),
              maybeOsRelease = nc.maybeOsRelease.map(OsRelease.apply),
              hostname = nc.hostname,
              additionalHostnames = Set.empty,
              interfaces = nc.interfaces.toSet
            )
          )
          result.hosts should have size 1
          result.connections shouldBe empty
        }
      }

      "append network utilization" in {
        // given
        forAll { (nu: Measurement.NetworkUtilization) =>

          // when
          val result = buildStats(nu)

          // then
          result.hosts shouldBe empty
          result.connections shouldBe empty
        }
      }
    }

    "built from a network configuration" should {

      // given
      val measurements: Seq[Measurement[Timing]] = Seq(
        Measurement.NetworkConfiguration(
          machineId =
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          maybeOsRelease = OsRelease
            .FileContents(
              """ID=id
                |VERSION_ID="version_id"
                |#VERSION_ID="wrong_version_id"
                |  #  PRETTY_NAME   =     "<wrong-pretty-name>"  
                | PRETTY_NAME   =     "<pretty-name>"  """.stripMargin
            )
            .some,
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
      )

      // when
      val result: MonitoredStats = buildStats(measurements: _*)

      "have a host id" in {
        // then
        result.hosts should have size 1
        result.hosts.head.hostId shouldBe HostId(
          MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228"))
        )
      }

      "have an os release" in {
        // then
        result.hosts should have size 1
        result.hosts.head.maybeOsRelease.value shouldBe OsRelease(
          maybeId = OsRelease.Id("id").some,
          maybeVersionId = OsRelease.VersionId("version_id").some,
          maybePrettyName = OsRelease.PrettyName("<pretty-name>").some
        )
      }

      "have the hostname" in {
        // then
        result.hosts should have size 1
        result.hosts.head.hostname shouldBe Hostname
          .fromString("some-host.example.com")
          .get
      }
    }

    "built from a network configuration with hostname collision according to the machine ids" should {

      // given
      val measurements: Seq[Measurement[Timing]] = Seq(
        Measurement.NetworkConfiguration(
          machineId =
            MachineId(UUID.fromString("0b7f7d58-3c5c-4f92-9ada-4c27ab19b195")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          maybeOsRelease = None,
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        ),
        Measurement.NetworkConfiguration(
          machineId =
            MachineId(UUID.fromString("0c8cb005-a78b-49c5-8760-48daf08ea86f")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          maybeOsRelease = None,
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
      )

      // when
      val result: MonitoredStats = buildStats(measurements: _*)

      "not merge hosts with the same hostname" in {
        // then
        result.hosts.foreach(host =>
          host.hostname shouldBe Hostname
            .fromString("some-host.example.com")
            .get
        )
        result.hosts should have size 2
      }
    }

    "built from a network configuration with multiple hostnames for the same host according to the machine ids" should {

      // given
      val measurements: Seq[Measurement[Timing]] = Seq(
        Measurement.NetworkConfiguration(
          machineId =
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:10:50.34957395Z")
          ),
          maybeOsRelease = None,
          hostname = Hostname.fromString("another-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        ),
        Measurement.NetworkConfiguration(
          machineId =
            MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228")),
          timing = Timing.Timestamp(
            ZonedDateTime.parse("2022-05-18T18:09:50.34957395Z")
          ),
          maybeOsRelease = None,
          hostname = Hostname.fromString("some-host.example.com").get,
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
      )

      // when
      val result: MonitoredStats = buildStats(measurements: _*)

      "merge hosts with the same agent id" in {
        // then
        result.hosts should have size 1
        result.hosts.head.hostId shouldBe HostId(
          MachineId(UUID.fromString("a814d0d9-3dca-4acf-985f-442dd4262228"))
        )
      }

      "have primary hostname from most recent data and keep track of other hostnames" in {
        // then
        result.hosts should have size 1
        val monitoredHosts = result.hosts
        monitoredHosts.head.hostname shouldBe Hostname
          .fromString("some-host.example.com")
          .get
        monitoredHosts.head.additionalHostnames should have size 1
        monitoredHosts.head.additionalHostnames should contain(
          Hostname.fromString("another-host.example.com").get
        )
      }
    }

    "built from a network configuration and network utilization" should {

      // given
      val machineId1 =
        MachineId(UUID.fromString("8cf801f1-5592-49ce-a5e3-256648dfd7ea"))
      val hostId1 = HostId(machineId1)
      val hostname1 = Hostname.fromString("some-host.example.com").get
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
          machineId = machineId1,
          timing = start1,
          maybeOsRelease = None,
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
          machineId = machineId2,
          timing = start2,
          maybeOsRelease = None,
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
          machineId = machineId1,
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
          machineId = machineId1,
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
          machineId = machineId2,
          timing = timeframe2a,
          connections = Seq.empty
        ),
        Measurement.NetworkUtilization(
          machineId = machineId2,
          timing = timeframe2b,
          connections = Seq.empty
        )
      )

      // when
      val result: MonitoredStats = buildStats(measurements: _*)

      "have all hosts" in {
        // then
        result.hosts.map(_.hostId) should contain theSameElementsAs Set(
          hostId1,
          hostId2
        )
      }

      "have all connections" in {
        // then
        val externalHost = Host.fromString("10.20.87.210").get
        val externalHostId = HostId(externalHost)

        result.connections should contain theSameElementsAs Set(
          hostId1 -> hostId2,
          hostId1 -> externalHostId
        )
      }
    }

    "dropping" should {
      "not keep host without update after drop" in {
        // given
        val ncTemplate = MeasurementFixtures.ncGen()
        val nuTemplate = MeasurementFixtures.nuGen()

        val baseTiming = ncTemplate.timestamp.instant

        val nc = ncTemplate.copy(
          timing = Timing.Timestamp(baseTiming),
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
        val nu = nuTemplate.copy(
          timing = Timing.Timeframe(
            Interval(baseTiming.plusMillis(2), nuTemplate.timing.duration)
          )
        )
        val stats: MonitoredStats = buildStats(nc, nu)
        val timestamp =
          Timing.Timestamp(baseTiming.plusSeconds(15))

        // when
        val result = stats.dropBefore(timestamp)

        // then
        result.hosts shouldBe empty
      }

      "keep host with configuration update after drop" in {
        // given
        val ncTemplate = MeasurementFixtures.ncGen()

        val baseTiming = ncTemplate.timestamp.instant

        val nc1 = ncTemplate.copy(
          timing = Timing.Timestamp(baseTiming),
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
        val nc2 = nc1.copy(
          timing = Timing.Timestamp(baseTiming.plusSeconds(60))
        )
        val stats: MonitoredStats = buildStats(nc1, nc2)
        val timestamp =
          Timing.Timestamp(baseTiming.plusSeconds(15))

        // when
        val result = stats.dropBefore(timestamp)

        // then
        result.hosts should not be empty
      }

      "keep host with utilization update timeframe ending after drop" in {
        // given
        val ncTemplate = MeasurementFixtures.ncGen()
        val nuTemplate =
          MeasurementFixtures.nuGen().copy(machineId = ncTemplate.machineId)

        val baseTiming = ncTemplate.timestamp.instant

        val nc = ncTemplate.copy(
          timing = Timing.Timestamp(baseTiming),
          interfaces = Seq.empty,
          openSockets = Seq.empty
        )
        val nu = nuTemplate.copy(
          timing = Timing.Timeframe(
            Interval(baseTiming.plusMillis(4), nuTemplate.timing.duration)
          )
        )
        val stats: MonitoredStats = buildStats(nc, nu)
        val timestamp =
          Timing.Timestamp(baseTiming.plusSeconds(5))

        // when
        val result = stats.dropBefore(timestamp)

        // then
        result.hosts should not be empty
      }

      "keep only connections from utilization update after drop" in {
        // given
        val timeframeDataset = MeasurementFixtures.TimeframeDataset.gen()
        val stats: MonitoredStats = buildStats(
          timeframeDataset.nc,
          timeframeDataset.nuBefore,
          timeframeDataset.nuAfter
        )

        // when
        val result = stats.dropBefore(timeframeDataset.cutoffTimestamp)

        // then
        result.connections.map(_._2) should contain theSameElementsAs Set(
          HostId(timeframeDataset.con2.remoteSocket.value.host),
          HostId(timeframeDataset.con3.remoteSocket.value.host)
        )
      }
    }
  }

  private def buildStats(measurements: Measurement[Timing]*): MonitoredStats =
    measurements.foldLeft(Stats.empty) { case (stats, measurement) =>
      stats.append(measurement)
    }
}

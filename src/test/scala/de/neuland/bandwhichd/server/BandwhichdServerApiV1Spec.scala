package de.neuland.bandwhichd.server

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Async, IO}
import cats.implicits.*
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.dimafeng.testcontainers.ForEachTestContainer
import de.neuland.bandwhichd.server.adapter.in.v1.message.{
  ApiV1MessageV1Fixtures,
  MessageController
}
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementsInMemoryRepository
import de.neuland.bandwhichd.server.boot.{
  App,
  Configuration,
  ConfigurationFixtures,
  Routes
}
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.domain.stats.Stats
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext
import de.neuland.bandwhichd.server.lib.test.cassandra.CassandraContainer
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import de.neuland.bandwhichd.server.lib.time.cats.TimeContextMocks.*
import de.neuland.bandwhichd.server.test.CassandraTestMigration
import io.circe.Json
import io.circe.Json.{arr, fromString, obj}
import org.http4s.*
import org.http4s.Status.{MethodNotAllowed, Ok}
import org.http4s.implicits.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.typelevel.ci.*

class BandwhichdServerApiV1Spec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers {

  private def configuration: Configuration = ConfigurationFixtures.testDefaults
  private def inMemoryApp[F[_]: Async](
      timeContext: TimeContext[F],
      configuration: Configuration
  ): App[F] = {
    new App[F](
      timeContext = timeContext,
      cassandraContext = null,
      configuration = configuration
    ) {
      override lazy val measurementsRepository: MeasurementsRepository[F] =
        new MeasurementsInMemoryRepository()
    }
  }

  "bandwhichd-server v1 API" should {
    "have health status" in {
      // given
      val request = Request[IO](
        method = Method.GET,
        uri = uri"/v1/health",
        headers = Headers(
          Header.Raw(
            ci"origin",
            "http://localhost:3000"
          )
        )
      )

      val httpApp = inMemoryApp[IO](
        unimplementedTimeContext,
        configuration
      ).httpApp

      for {
        // when
        result <- httpApp.run(request)

        // then
        body <- result.body.through(fs2.text.utf8.decode).compile.string
      } yield {
        result.status shouldBe Ok
        result.headers.headers should contain allOf (
          Header.Raw(ci"access-control-allow-origin", "*"),
          Header.Raw(ci"content-type", "application/json")
        )
        val jsonBody = io.circe.parser.parse(body).toTry.get
        jsonBody.isObject shouldBe true
        jsonBody.asObject.get.keys should contain("status")
        jsonBody.asObject.get("status").get.isString shouldBe true
        jsonBody.asObject.get("status").get.asString.get shouldBe "pass"
        jsonBody.asObject.get.keys should contain("checks")
        jsonBody.asObject.get("checks").get.isObject shouldBe true
        val checks = jsonBody.asObject.get("checks").get.asObject
        checks.get.keys should contain("memory:utilization")
        checks.get("memory:utilization").get.isArray shouldBe true
        checks
          .get("memory:utilization")
          .get
          .asArray
          .get should have length 1
        checks
          .get("memory:utilization")
          .get
          .asArray
          .get
          .head
          .isObject shouldBe true
        val firstMemoryUtilization = checks
          .get("memory:utilization")
          .get
          .asArray
          .get
          .head
          .asObject
          .get
        firstMemoryUtilization.keys should contain("status")
        firstMemoryUtilization("status").get.isString shouldBe true
        firstMemoryUtilization("status").get.asString.get shouldBe "pass"
      }
    }

    "record message v1" in {
      // given
      val request = Request[IO](
        method = Method.POST,
        uri = uri"/v1/messages",
        entity = EntityEncoder.stringEncoder.toEntity(
          ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJson
        )
      )

      val httpApp = inMemoryApp[IO](
        fixedTimeContext(MeasurementFixtures.fullTimeframe.end.instant),
        configuration
      ).httpApp

      for {
        // when
        result <- httpApp.run(request)
      } yield {

        // then
        result.status shouldBe Ok
      }
    }

    "reject recording message v1" in {
      // given
      val readOnlyConfiguration = configuration.copy(readonly = true)

      val request = Request[IO](
        method = Method.POST,
        uri = uri"/v1/messages",
        entity = EntityEncoder.stringEncoder.toEntity(
          ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJson
        )
      )

      val app = inMemoryApp[IO](
        unimplementedTimeContext,
        readOnlyConfiguration
      )
      val httpApp = app.httpApp

      for {
        // when
        result <- httpApp.run(request)

        // then
        measurements <- app.measurementsRepository
          .get(MeasurementFixtures.fullTimeframe)
          .compile
          .toList
      } yield {
        result.status shouldBe MethodNotAllowed
        measurements shouldBe empty
      }
    }

    "have recordings" in {
      // given
      val request = Request[IO](
        method = Method.GET,
        uri = uri"/v1/messages",
        headers = Headers(
          Header.Raw(
            ci"origin",
            "http://localhost:3000"
          )
        )
      )

      val app = inMemoryApp[IO](
        fixedTimeContext(MeasurementFixtures.fullTimeframe.end.instant),
        configuration
      )
      val httpApp = app.httpApp

      for {
        _ <- app.measurementsRepository
          .record(MeasurementFixtures.exampleNetworkConfigurationMeasurement)
        _ <- app.measurementsRepository
          .record(MeasurementFixtures.exampleNetworkUtilizationMeasurement)

        // when
        result <- httpApp.run(request)

        // then
        body <- result.body.through(fs2.text.utf8.decode).compile.string
      } yield {
        result.status shouldBe Ok
        result.headers.headers should contain allOf (
          Header.Raw(ci"access-control-allow-origin", "*"),
          Header.Raw(ci"content-type", "application/json")
        )
        body shouldBe s"[${ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJsonNoSpaces},${ApiV1MessageV1Fixtures.exampleNetworkUtilizationMeasurementJsonNoSpaces}]"
      }
    }

    "have recordings filtered by time" in {
      // given
      val request = Request[IO](
        method = Method.GET,
        uri =
          uri"/v1/messages?from=2022-05-06T15:14:51.742Z&to=2022-05-06T15:14:51.842Z",
        headers = Headers(
          Header.Raw(
            ci"origin",
            "http://localhost:3000"
          )
        )
      )

      val app = inMemoryApp[IO](
        unimplementedTimeContext,
        configuration
      )
      val httpApp = app.httpApp

      for {
        _ <- app.measurementsRepository
          .record(MeasurementFixtures.exampleNetworkConfigurationMeasurement)
        _ <- app.measurementsRepository
          .record(MeasurementFixtures.exampleNetworkUtilizationMeasurement)

        // when
        result <- httpApp.run(request)

        // then
        body <- result.body.through(fs2.text.utf8.decode).compile.string
      } yield {
        result.status shouldBe Ok
        result.headers.headers should contain allOf (
          Header.Raw(ci"access-control-allow-origin", "*"),
          Header.Raw(ci"content-type", "application/json")
        )
        body shouldBe s"[${ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJsonNoSpaces}]"
      }
    }

    "have stats" in {
      // given
      val request = Request[IO](
        method = Method.GET,
        uri = uri"/v1/stats",
        headers = Headers(
          Header.Raw(
            ci"origin",
            "http://localhost:3000"
          )
        )
      )

      val app = inMemoryApp[IO](
        fixedTimeContext(MeasurementFixtures.fullTimeframe.end.instant),
        configuration
      )
      val httpApp = app.httpApp

      for {
        _ <- app.measurementApplicationService
          .record(MeasurementFixtures.exampleNetworkConfigurationMeasurement)
        _ <- app.measurementApplicationService
          .record(MeasurementFixtures.exampleNetworkUtilizationMeasurement)

        // when
        result <- httpApp.run(request)

        // then
        body <- result.body.through(fs2.text.utf8.decode).compile.string
      } yield {
        result.status shouldBe Ok
        result.headers.headers should contain allOf (
          Header.Raw(ci"access-control-allow-origin", "*"),
          Header.Raw(ci"content-type", "application/json")
        )
        val jsonBody = io.circe.parser.parse(body).toTry.get
        jsonBody shouldBe obj(
          "hosts" -> obj(
            "c414c2da-714c-4b68-b97e-3f31e18053d2" -> obj(
              "os_release" -> obj(
                "pretty_name" -> fromString("Debian GNU/Linux 11 (bullseye)"),
                "version_id" -> fromString("11"),
                "id" -> fromString("debian")
              ),
              "hostname" -> fromString("some-host.example.com"),
              "additional_hostnames" -> arr(),
              "connections" -> obj(
                "c414c2da-714c-4b68-b97e-3f31e18053d2" -> obj(),
                "959619ee-30a2-3bc8-9b79-4384b5f3f05d" -> obj()
              )
            )
          ),
          "unmonitoredHosts" -> obj(
            "959619ee-30a2-3bc8-9b79-4384b5f3f05d" -> obj(
              "host" -> fromString("192.168.10.34")
            )
          )
        )
      }
    }

    "have stats filtered by time" in {
      // given
      val timeframeDataset = MeasurementFixtures.TimeframeDataset.gen()
      val from = timeframeDataset.cutoffTimestamp.instant
      val to = from.plus(Stats.defaultTimeframeDuration)
      val request = Request[IO](
        method = Method.GET,
        uri = Uri.unsafeFromString(s"/v1/stats?from=$from&to=$to"),
        headers = Headers(
          Header.Raw(
            ci"origin",
            "http://localhost:3000"
          )
        )
      )

      val app = inMemoryApp[IO](
        unimplementedTimeContext,
        configuration
      )
      val httpApp = app.httpApp

      for {
        _ <- timeframeDataset.measurements.traverse(
          app.measurementsRepository.record
        )

        // when
        result <- httpApp.run(request)

        // then
        body <- result.body.through(fs2.text.utf8.decode).compile.string
      } yield {
        result.status shouldBe Ok
        result.headers.headers should contain allOf (
          Header.Raw(ci"access-control-allow-origin", "*"),
          Header.Raw(ci"content-type", "application/json")
        )
        val jsonBody = io.circe.parser.parse(body).toTry.get
        jsonBody shouldBe obj(
          "hosts" -> obj(
            timeframeDataset.hostId0.uuid.toString -> obj(
              "hostname" -> fromString(timeframeDataset.nc.hostname.toString),
              "additional_hostnames" -> arr(),
              "connections" -> obj(
                timeframeDataset.hostId2.uuid.toString -> obj(),
                timeframeDataset.hostId3.uuid.toString -> obj()
              )
            )
          ),
          "unmonitoredHosts" -> obj(
            timeframeDataset.hostId2.uuid.toString -> obj(
              "host" -> fromString(
                timeframeDataset.con2.remoteSocket.value.host.toString
              )
            ),
            timeframeDataset.hostId3.uuid.toString -> obj(
              "host" -> fromString(
                timeframeDataset.con3.remoteSocket.value.host.toString
              )
            )
          )
        )
      }
    }
  }
}

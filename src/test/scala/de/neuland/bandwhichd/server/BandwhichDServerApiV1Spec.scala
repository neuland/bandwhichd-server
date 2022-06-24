package de.neuland.bandwhichd.server

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.dimafeng.testcontainers.ForAllTestContainer
import de.neuland.bandwhichd.server.adapter.in.v1.message.{
  ApiV1MessageV1Fixtures,
  MessageController
}
import de.neuland.bandwhichd.server.boot.{
  App,
  Configuration,
  ConfigurationFixtures,
  Routes
}
import de.neuland.bandwhichd.server.domain.measurement.MeasurementFixtures
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext
import de.neuland.bandwhichd.server.lib.test.cassandra.CassandraContainer
import de.neuland.bandwhichd.server.test.CassandraTestMigration
import io.circe.Json
import io.circe.Json.{arr, fromString, obj}
import org.http4s.*
import org.http4s.Status.Ok
import org.http4s.implicits.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.typelevel.ci.*

class BandwhichDServerApiV1Spec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with ForAllTestContainer {

  override val container: CassandraContainer = CassandraContainer()
  private def configuration: Configuration =
    ConfigurationFixtures.testDefaults(container)

  "bandwhichd-server v1 API" should {
    "have health status" in {
      CassandraContext
        .resource[IO](configuration)
        .use { cassandraContext =>

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

          val httpApp = App[IO](cassandraContext, configuration).httpApp

          for {
            _ <- CassandraTestMigration(cassandraContext).migrate(configuration)

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
    }

    "record message v1" in {
      CassandraContext.resource[IO](configuration).use { cassandraContext =>

        // given
        val request = Request[IO](
          method = Method.POST,
          uri = uri"/v1/message",
          entity = EntityEncoder.stringEncoder.toEntity(
            ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJson
          )
        )

        val httpApp = App[IO](cassandraContext, configuration).httpApp

        for {
          _ <- CassandraTestMigration(cassandraContext).migrate(configuration)

          // when
          result <- httpApp.run(request)
        } yield {

          // then
          result.status shouldBe Ok
        }
      }
    }

    "have JSON stats" in {
      CassandraContext.resource[IO](configuration).use { cassandraContext =>

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

        val app = App[IO](cassandraContext, configuration)
        val httpApp = app.httpApp

        for {
          _ <- CassandraTestMigration(cassandraContext).migrate(configuration)

          _ <- app.measurementRepository
            .record(MeasurementFixtures.exampleNetworkConfigurationMeasurement)
          _ <- app.measurementRepository
            .record(MeasurementFixtures.exampleNetworkUtilizationMeasurement)
          aggregationSchedule <- app.aggregationScheduler.schedule
          _ <- aggregationSchedule.work.run

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
                "hostname" -> fromString("some-host.example.com"),
                "additional_hostnames" -> arr()
              )
            )
          )
        }
      }
    }

    "have DOT stats" in {
      CassandraContext.resource[IO](configuration).use { cassandraContext =>

        // given
        val request = Request[IO](
          method = Method.GET,
          uri = uri"/v1/stats",
          headers = Headers(
            Header.Raw(
              ci"accept",
              "application/json; q=0.8, text/vnd.graphviz; q=0.9"
            ),
            Header.Raw(
              ci"origin",
              "http://localhost:3000"
            )
          )
        )

        val app = App[IO](cassandraContext, configuration)
        val httpApp = app.httpApp

        for {
          _ <- CassandraTestMigration(cassandraContext).migrate(configuration)

          _ <- app.measurementRepository.record(
            MeasurementFixtures.exampleNetworkConfigurationMeasurement
          )
          _ <- app.measurementRepository.record(
            MeasurementFixtures.exampleNetworkUtilizationMeasurement
          )
          aggregationSchedule <- app.aggregationScheduler.schedule
          _ <- aggregationSchedule.work.run

          // when
          result <- httpApp.run(request)

          // then
          body <- result.body.through(fs2.text.utf8.decode).compile.string
        } yield {
          result.status shouldBe Ok
          result.headers.headers should contain allOf (
            Header.Raw(ci"access-control-allow-origin", "*"),
            Header.Raw(ci"content-type", "text/vnd.graphviz; charset=UTF-8")
          )
          body shouldBe
            """digraph {
                |    "c414c2da-714c-4b68-b97e-3f31e18053d2" [label="some-host.example.com"];
                |    "959619ee-30a2-3bc8-9b79-4384b5f3f05d" [label="192.168.10.34"];
                |    "c414c2da-714c-4b68-b97e-3f31e18053d2" -> "c414c2da-714c-4b68-b97e-3f31e18053d2";
                |    "c414c2da-714c-4b68-b97e-3f31e18053d2" -> "959619ee-30a2-3bc8-9b79-4384b5f3f05d";
                |}""".stripMargin
        }
      }
    }
  }
}

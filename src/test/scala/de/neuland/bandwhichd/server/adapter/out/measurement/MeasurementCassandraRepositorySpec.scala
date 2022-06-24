package de.neuland.bandwhichd.server.adapter.out.measurement

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, SimpleStatement}
import com.dimafeng.testcontainers.ForEachTestContainer
import de.neuland.bandwhichd.server.adapter.out.CassandraMigration
import de.neuland.bandwhichd.server.boot.{Configuration, ConfigurationFixtures}
import de.neuland.bandwhichd.server.domain.measurement.{
  MeasurementFixtures,
  MeasurementRepository,
  Timing
}
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext
import de.neuland.bandwhichd.server.lib.test.cassandra.CassandraContainer
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.test.CassandraTestMigration
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AsyncWordSpec}
import org.scalatest.{Assertion, EitherValues}
import org.testcontainers.containers.GenericContainer

class MeasurementCassandraRepositorySpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with EitherValues
    with ForEachTestContainer {

  override val container: CassandraContainer = CassandraContainer()
  private def configuration: Configuration =
    ConfigurationFixtures.testDefaults(container)

  "MeasurementCassandraRepository" should {
    "record and get measurements" in {
      CassandraContext.resource[IO](configuration).use { cassandraContext =>

        // given
        val a = MeasurementFixtures.exampleNetworkConfigurationMeasurement
        val b = MeasurementFixtures.exampleNetworkUtilizationMeasurement
        val c = MeasurementFixtures.exampleNetworkUtilizationMeasurement
          .copy(timing =
            Timing.Timeframe(
              Interval(
                b.timing.value.normalizedStop,
                b.timing.value.normalizedDuration
              )
            )
          )

        val measurementRepository: MeasurementRepository[IO] =
          MeasurementCassandraRepository[IO](
            cassandraContext,
            configuration
          )

        for {
          _ <- CassandraTestMigration(cassandraContext)
            .migrate(configuration)

          // when
          result0 <- measurementRepository.getAll.compile.toList
          _ <- measurementRepository.record(a)
          result1 <- measurementRepository.getAll.compile.toList
          _ <- measurementRepository.record(c)
          result2 <- measurementRepository.getAll.compile.toList
          _ <- measurementRepository.record(b)
          result3 <- measurementRepository.getAll.compile.toList

        } yield {
          // then
          result0 shouldBe empty
          result1 shouldBe List(a)
          result2 shouldBe List(a, c)
          result3 shouldBe List(a, b, c)
        }
      }
    }

    "reject recording" in {
      // given
      val readOnlyConfiguration = configuration.copy(readonly = true)
      CassandraContext.resource[IO](readOnlyConfiguration).use {
        cassandraContext =>

          val a = MeasurementFixtures.exampleNetworkConfigurationMeasurement

          val measurementRepository: MeasurementRepository[IO] =
            MeasurementCassandraRepository[IO](
              cassandraContext,
              readOnlyConfiguration
            )

          for {
            _ <- CassandraTestMigration(cassandraContext)
              .migrate(readOnlyConfiguration)

            // when
            result <- measurementRepository.record(a).attempt

            // then
            measurements <- measurementRepository.getAll.compile.toList
          } yield {
            result.left.value should have message "readonly mode enabled"
            measurements shouldBe empty
          }
      }
    }
  }
}
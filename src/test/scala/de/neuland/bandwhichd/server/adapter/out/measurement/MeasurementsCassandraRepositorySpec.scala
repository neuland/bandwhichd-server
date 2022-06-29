package de.neuland.bandwhichd.server.adapter.out.measurement

import cats.data.NonEmptySeq
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
  MeasurementsRepository,
  Timing
}
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext
import de.neuland.bandwhichd.server.lib.test.cassandra.CassandraContainer
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.test.CassandraTestMigration
import org.scalatest.matchers.should.Matchers
import org.scalatest.tagobjects.Slow
import org.scalatest.wordspec.{AnyWordSpec, AsyncWordSpec}
import org.scalatest.{Assertion, EitherValues}
import org.testcontainers.containers.GenericContainer

class MeasurementsCassandraRepositorySpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with EitherValues
    with ForEachTestContainer {

  override val container: CassandraContainer = CassandraContainer()
  private def configuration: Configuration =
    ConfigurationFixtures.testDefaults(container)

  "MeasurementCassandraRepository" should {
    "record and get measurements" taggedAs Slow in {
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
        val timeframe = Timing.Timeframe(
          NonEmptySeq(
            c.timing.end,
            MeasurementFixtures.allTimestamps
          )
        )

        val measurementsRepository: MeasurementsRepository[IO] =
          MeasurementsCassandraRepository[IO](
            cassandraContext,
            configuration
          )

        for {
          _ <- CassandraTestMigration(cassandraContext)
            .migrate(configuration)

          // when
          result0 <- measurementsRepository.get(timeframe).compile.toList
          _ <- measurementsRepository.record(a)
          result1 <- measurementsRepository.get(timeframe).compile.toList
          _ <- measurementsRepository.record(c)
          result2 <- measurementsRepository.get(timeframe).compile.toList
          _ <- measurementsRepository.record(b)
          result3 <- measurementsRepository.get(timeframe).compile.toList

        } yield {
          // then
          result0 shouldBe empty
          result1 shouldBe List(a)
          result2 shouldBe List(a, c)
          result3 shouldBe List(a, b, c)
        }
      }
    }

    "reject recording" taggedAs Slow in {
      // given
      val readOnlyConfiguration = configuration.copy(readonly = true)
      CassandraContext.resource[IO](readOnlyConfiguration).use {
        cassandraContext =>

          val a = MeasurementFixtures.exampleNetworkConfigurationMeasurement

          val measurementsRepository: MeasurementsRepository[IO] =
            MeasurementsCassandraRepository[IO](
              cassandraContext,
              readOnlyConfiguration
            )

          for {
            _ <- CassandraTestMigration(cassandraContext)
              .migrate(readOnlyConfiguration)

            // when
            result <- measurementsRepository.record(a).attempt

            // then
            measurements <- measurementsRepository
              .get(MeasurementFixtures.fullTimeframe)
              .compile
              .toList
          } yield {
            result.left.value should have message "readonly mode enabled"
            measurements shouldBe empty
          }
      }
    }
  }
}

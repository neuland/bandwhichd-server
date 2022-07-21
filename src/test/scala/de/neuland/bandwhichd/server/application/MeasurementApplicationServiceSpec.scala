package de.neuland.bandwhichd.server.application

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.comcast.ip4s.*
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementsInMemoryRepository
import de.neuland.bandwhichd.server.adapter.out.stats.StatsInMemoryRepository
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.time.cats.TimeContextMocks
import de.neuland.bandwhichd.server.lib.time.cats.TimeContextMocks.fixedTimeContext
import de.neuland.bandwhichd.server.test.Arbitraries.{sample, given}
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AsyncWordSpec}

class MeasurementApplicationServiceSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers {

  "MeasurementApplicationService" should {
    "drop outdated and not updated data when recording messages" in {
      val nc1 = ncGen().copy(
        hostname = host"host1"
      )
      val nc2 = ncGen().copy(
        timing = Timing.Timestamp(
          nc1.timing.instant
            .plus(Stats.defaultTimeframeDuration)
        ),
        hostname = host"host2"
      )
      val nc3 = ncGen().copy(
        timing = Timing.Timestamp(
          nc1.timing.instant
            .plus(Stats.defaultTimeframeDuration.multipliedBy(2))
        ),
        hostname = host"host3"
      )

      val now =
        nc2.timing.instant.plus(Stats.defaultTimeframeDuration.dividedBy(2))

      val statsRepository = new StatsInMemoryRepository[IO]()

      val service = new MeasurementApplicationService[IO](
        measurementsRepository = new MeasurementsInMemoryRepository(),
        statsRepository = statsRepository,
        timeContext = fixedTimeContext(now)
      )

      for {
        // given
        _ <- statsRepository.update(_.append(nc1))
        _ <- statsRepository.update(_.append(nc2))

        // when
        _ <- service.record(nc3)
        result <- statsRepository.get

        // then
      } yield {
        result.hosts.map(_.hostname) should contain theSameElementsAs Set(
          host"host2",
          host"host3"
        )
      }
    }
  }

  private def ncGen = sample[Measurement.NetworkConfiguration]
}

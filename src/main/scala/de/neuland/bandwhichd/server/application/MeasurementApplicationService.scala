package de.neuland.bandwhichd.server.application

import cats.Monad
import cats.effect.Sync
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import fs2.Stream

class MeasurementApplicationService[F[_]: Sync](
    private val measurementsRepository: MeasurementsRepository[F],
    private val statsRepository: StatsRepository[F],
    private val timeContext: TimeContext[F]
) {
  def get(timeframe: Timing.Timeframe): Stream[F, Measurement[Timing]] =
    measurementsRepository.get(timeframe)

  def record(measurement: Measurement[Timing]): F[Unit] =
    for {
      _ <- measurementsRepository.record(measurement)
      now <- timeContext.now
      _ <- statsRepository.update { stats =>
        stats
          .append(measurement)
          .dropBefore(
            Timing.Timestamp(now.minus(Stats.defaultTimeframeDuration))
          )
      }
    } yield ()
}

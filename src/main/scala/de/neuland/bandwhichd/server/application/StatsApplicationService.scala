package de.neuland.bandwhichd.server.application

import cats.effect.kernel.Concurrent
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.MeasurementRepository
import de.neuland.bandwhichd.server.domain.stats.{Stats, StatsRepository}
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext

class StatsApplicationService[F[_]: Concurrent](
    private val timeContext: TimeContext[F],
    private val measurementRepository: MeasurementRepository[F],
    private val statsRepository: StatsRepository[F]
) {
  def get: F[Stats] =
    statsRepository.get

  def recalculate: F[Unit] =
    for {
      defaultTimeframe <- Stats.defaultTimeframe(timeContext)
      stats <- Stats(measurementRepository.get(defaultTimeframe))
      _ <- statsRepository.safe(stats)
    } yield ()
}

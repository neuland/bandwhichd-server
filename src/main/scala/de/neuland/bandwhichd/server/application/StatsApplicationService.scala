package de.neuland.bandwhichd.server.application

import cats.Monad
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.MeasurementRepository
import de.neuland.bandwhichd.server.domain.stats.{Stats, StatsRepository}

class StatsApplicationService[F[_]: Monad](
    private val measurementRepository: MeasurementRepository[F],
    private val statsRepository: StatsRepository[F]
) {
  def get: F[Stats] =
    statsRepository.get

  def recalculate: F[Unit] = {
    for {
      measurements <- measurementRepository.getAll
      stats <- Stats(measurements)
      _ <- statsRepository.safe(stats)
    } yield ()
  }
}

package de.neuland.bandwhichd.server.application

import cats.effect.kernel.Concurrent
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.MeasurementsRepository
import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext

class StatsApplicationService[F[_]: Concurrent](
    private val statsRepository: StatsRepository[F]
) {
  def get: F[MonitoredStats] =
    statsRepository.get
}

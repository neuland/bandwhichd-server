package de.neuland.bandwhichd.server.application

import cats.effect.kernel.Concurrent
import cats.implicits.*
import de.neuland.bandwhichd.server.boot.Configuration
import de.neuland.bandwhichd.server.domain.measurement.*
import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext

class StatsApplicationService[F[_]: Concurrent](
    private val configuration: Configuration,
    private val statsRepository: StatsRepository[F],
    private val measurementsRepository: MeasurementsRepository[F]
) {
  def get(maybeTimeframe: Option[Timing.Timeframe]): F[MonitoredStats] =
    maybeTimeframe.fold(statsRepository.get) { timeframe =>
      val extendedTimeframeIncludingRequiredNetworkConfiguration =
        Timing.Timeframe(
          Interval(
            start = timeframe.start.instant.minus(
              configuration.maximumTimespanBetweenNetworkConfigurationUpdates
            ),
            stop = timeframe.end.instant
          )
        )

      for {
        stats <- measurementsRepository
          .get(extendedTimeframeIncludingRequiredNetworkConfiguration)
          .compile
          .fold(Stats.empty) { case (stats, measurement) =>
            stats.append(measurement)
          }
      } yield stats.dropBefore(timeframe.start)
    }
}

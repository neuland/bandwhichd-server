package de.neuland.bandwhichd.server.adapter.in.scheduler

import cats.Monad
import de.neuland.bandwhichd.server.application.StatsApplicationService
import de.neuland.bandwhichd.server.lib.scheduling.{Schedule, Scheduler, Work}

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import de.neuland.bandwhichd.server.boot.Configuration
import scala.jdk.DurationConverters.*

class AggregationScheduler[F[_]: Monad](
    private val configuration: Configuration,
    private val statsApplicationService: StatsApplicationService[F]
) extends Scheduler[F] {
  override def schedule: F[Schedule[F]] =
    Monad[F].pure(
      Schedule.Pausing(
        getClass.getSimpleName,
        configuration.aggregationSchedulerInterval.toScala,
        Work(statsApplicationService.recalculate)
      )
    )
}

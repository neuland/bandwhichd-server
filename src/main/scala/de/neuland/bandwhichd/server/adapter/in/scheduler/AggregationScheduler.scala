package de.neuland.bandwhichd.server.adapter.in.scheduler

import cats.Monad
import de.neuland.bandwhichd.server.application.StatsApplicationService
import de.neuland.bandwhichd.server.lib.scheduling.{Schedule, Scheduler, Work}

import scala.concurrent.duration.{FiniteDuration, SECONDS}

class AggregationScheduler[F[_]: Monad](
    private val statsApplicationService: StatsApplicationService[F]
) extends Scheduler[F] {
  override def schedule: F[Schedule[F]] =
    Monad[F].pure(
      Schedule.Pausing(
        FiniteDuration(10, SECONDS),
        Work({ statsApplicationService.recalculate })
      )
    )
}

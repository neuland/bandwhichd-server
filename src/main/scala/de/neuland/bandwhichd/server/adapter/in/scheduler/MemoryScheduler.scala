package de.neuland.bandwhichd.server.adapter.in.scheduler

import cats.Monad
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementInMemoryRepository
import de.neuland.bandwhichd.server.lib.health.jvm.JvmMemoryUtilization
import de.neuland.bandwhichd.server.lib.scheduling.{Schedule, Scheduler, Work}

import scala.concurrent.duration.{FiniteDuration, SECONDS}

class MemoryScheduler[F[_]: Monad](
    private val measurementInMemoryRepository: MeasurementInMemoryRepository[F]
) extends Scheduler[F] {

  private val inMemoryStorageCapThreshold: Long =
    scala.util.Properties
      .envOrElse("IN_MEMORY_STORAGE_CAP_THRESHOLD", "")
      .toLongOption
      .getOrElse(80)

  override def schedule: F[Schedule[F]] =
    Monad[F].pure(
      Schedule.Pausing(
        FiniteDuration(10, SECONDS),
        Work({
          if (
            JvmMemoryUtilization.current.usedMemoryPercentage > inMemoryStorageCapThreshold
          ) {
            measurementInMemoryRepository.cap
          } else {
            Monad[F].pure(())
          }
        })
      )
    )
}

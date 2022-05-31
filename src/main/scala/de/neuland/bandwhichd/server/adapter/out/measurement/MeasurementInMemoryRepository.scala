package de.neuland.bandwhichd.server.adapter.out.measurement

import cats.Monad
import cats.effect.Sync
import cats.effect.kernel.Concurrent
import de.neuland.bandwhichd.server.domain.measurement.{
  Measurement,
  MeasurementRepository,
  Timing
}

import java.util.concurrent.atomic.AtomicReference

class MeasurementInMemoryRepository[F[_]: Sync]
    extends MeasurementRepository[F] {
  private val storage: AtomicReference[Seq[Measurement[Timing]]] =
    new AtomicReference(Seq.empty)

  override def record(measurement: Measurement[Timing]): F[Unit] =
    Sync[F].blocking {
      val _ =
        storage.updateAndGet(measurements => measurements.appended(measurement))
      ()
    }

  override def getAll: F[Seq[Measurement[Timing]]] =
    Sync[F].blocking {
      storage.get()
    }
}

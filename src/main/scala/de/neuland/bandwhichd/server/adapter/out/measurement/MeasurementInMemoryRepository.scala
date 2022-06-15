package de.neuland.bandwhichd.server.adapter.out.measurement

import cats.Monad
import cats.effect.Sync
import cats.effect.kernel.Concurrent
import de.neuland.bandwhichd.server.adapter.out.CappedStorage
import de.neuland.bandwhichd.server.domain.measurement.{
  Measurement,
  MeasurementRepository,
  Timing
}
import de.neuland.bandwhichd.server.lib.health.jvm.JvmMemoryUtilization
import de.neuland.bandwhichd.server.lib.health.{Check, CheckKey}
import io.circe.{Json, JsonObject}

import java.util.concurrent.atomic.AtomicReference

class MeasurementInMemoryRepository[F[_]: Sync]
    extends MeasurementRepository[F]
    with Check {
  private val storage: AtomicReference[CappedStorage[Measurement[Timing]]] =
    new AtomicReference(CappedStorage.empty)

  override def record(measurement: Measurement[Timing]): F[Unit] =
    Sync[F].blocking {
      val _ = storage.updateAndGet(_.store(measurement))
      ()
    }

  override def getAll: F[Seq[Measurement[Timing]]] =
    Sync[F].blocking {
      storage.get().storage.toSeq
    }

  override def key: CheckKey =
    CheckKey("in-memory-storage:utilization")

  override def value: JsonObject =
    JsonObject(
      "componentType" -> Json.fromString("datastore"),
      "observedValue" -> Json.fromInt(storage.get().storage.length),
      "observedUnit" -> Json.fromString("objects")
    )

  def cap: F[Unit] =
    Sync[F].blocking {
      val _ = storage.updateAndGet(_.cap)
      ()
    }
}

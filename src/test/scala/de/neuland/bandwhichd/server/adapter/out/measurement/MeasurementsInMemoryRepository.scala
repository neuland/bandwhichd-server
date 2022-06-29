package de.neuland.bandwhichd.server.adapter.out.measurement

import cats.effect.Sync
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementsInMemoryRepository.ByDateKey
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementsInMemoryRepository.byDateKey
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.*
import fs2.Stream

import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.SeqMap

class MeasurementsInMemoryRepository[F[_]: Sync]
    extends MeasurementsRepository[F] {

  private val byDateStore: AtomicReference[
    SeqMap[ByDateKey, Measurement[Timing]]
  ] = new AtomicReference(SeqMap.empty)

  override def record(measurement: Measurement[Timing]): F[Unit] =
    Sync[F].blocking {
      val _ = byDateStore.updateAndGet { seqMap =>
        seqMap.updated(measurement.byDateKey, measurement)
      }
      ()
    }

  override def get(
      timeframe: Timing.Timeframe
  ): Stream[F, Measurement[Timing]] =
    Stream
      .emits(byDateStore.get().values.toSeq.sortBy(_.timestamp))
      .filter(measurement => timeframe.contains(measurement.timestamp))
}

private object MeasurementsInMemoryRepository {
  enum MeasurementType {
    case NetworkConfiguration, NetworkUtilization
  }

  type ByDateKey = (Timing.Timestamp, AgentId, MeasurementType)

  extension (measurement: Measurement[Timing]) {
    def byDateKey: ByDateKey =
      (
        measurement.timestamp,
        measurement.agentId,
        measurement.`type`
      )

    def `type`: MeasurementType =
      measurement match
        case _: Measurement.NetworkConfiguration =>
          MeasurementType.NetworkConfiguration
        case _: Measurement.NetworkUtilization =>
          MeasurementType.NetworkUtilization
  }
}

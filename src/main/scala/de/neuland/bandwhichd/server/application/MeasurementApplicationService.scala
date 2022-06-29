package de.neuland.bandwhichd.server.application

import cats.Monad
import cats.effect.Sync
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.{
  Measurement,
  MeasurementRepository,
  Timing
}
import fs2.Stream

class MeasurementApplicationService[F[_]: Sync](
    private val measurementRepository: MeasurementRepository[F]
) {
  def get(timeframe: Timing.Timeframe): Stream[F, Measurement[Timing]] =
    measurementRepository.get(timeframe)

  def recordMeasurement(
      recordMeasurementCommand: RecordMeasurementCommand
  ): F[Unit] =
    measurementRepository.record(recordMeasurementCommand.measurement)
}

package de.neuland.bandwhichd.server.application

import cats.Monad
import cats.effect.Sync
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.{
  Measurement,
  MeasurementsRepository,
  Timing
}
import fs2.Stream

class MeasurementApplicationService[F[_]: Sync](
    private val measurementsRepository: MeasurementsRepository[F]
) {
  def get(timeframe: Timing.Timeframe): Stream[F, Measurement[Timing]] =
    measurementsRepository.get(timeframe)

  def recordMeasurement(
      recordMeasurementCommand: RecordMeasurementCommand
  ): F[Unit] =
    measurementsRepository.record(recordMeasurementCommand.measurement)
}

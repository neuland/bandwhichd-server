package de.neuland.bandwhichd.server.application

import cats.Monad
import cats.effect.Sync
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.MeasurementRepository

class MeasurementApplicationService[F[_]: Sync](
    private val measurementRepository: MeasurementRepository[F]
) {
  def recordMeasurement(
      recordMeasurementCommand: RecordMeasurementCommand
  ): F[Unit] =
    measurementRepository.record(recordMeasurementCommand.measurement)
}

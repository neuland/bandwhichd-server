package de.neuland.bandwhichd.server.domain.measurement

import fs2.Stream

trait MeasurementRepository[F[_]] {
  def record(measurement: Measurement[Timing]): F[Unit]

  def getAll: Stream[F, Measurement[Timing]]
}

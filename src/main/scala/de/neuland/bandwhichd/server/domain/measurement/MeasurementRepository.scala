package de.neuland.bandwhichd.server.domain.measurement

trait MeasurementRepository[F[_]] {
  def record(measurement: Measurement[Timing]): F[Unit]

  def getAll: F[Seq[Measurement[Timing]]]
}

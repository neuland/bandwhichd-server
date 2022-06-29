package de.neuland.bandwhichd.server.domain.measurement

import fs2.Stream

trait MeasurementsRepository[F[_]] {
  def record(measurement: Measurement[Timing]): F[Unit]

  def get(timeframe: Timing.Timeframe): Stream[F, Measurement[Timing]]
}

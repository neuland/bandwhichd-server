package de.neuland.bandwhichd.server.application

import de.neuland.bandwhichd.server.domain.measurement.{Measurement, Timing}

case class RecordMeasurementCommand(measurement: Measurement[Timing])

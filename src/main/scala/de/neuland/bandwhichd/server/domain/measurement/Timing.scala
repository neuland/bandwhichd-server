package de.neuland.bandwhichd.server.domain.measurement

import de.neuland.bandwhichd.server.lib.time.ZonedInterval

import java.time.{Duration, ZonedDateTime}

sealed trait Timing

object Timing {
  case class Timestamp(value: ZonedDateTime) extends Timing

  case class Timeframe(value: ZonedInterval) extends Timing {

    def start: Timestamp =
      Timestamp(value = value.normalizedStart)

    def duration: Duration =
      value.normalizedDuration

    def end: Timestamp =
      Timestamp(value = value.normalizedStop)
  }

  object Timeframe {

    def apply(start: Timestamp, duration: Duration): Timeframe =
      Timeframe(value = ZonedInterval(start.value, duration))

    def apply(start: ZonedDateTime, duration: Duration): Timeframe =
      Timeframe(value = ZonedInterval(start, duration))
  }
}

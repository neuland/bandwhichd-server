package de.neuland.bandwhichd.server.domain.measurement

import cats.data.NonEmptySeq
import de.neuland.bandwhichd.server.lib.time.Interval

import java.time.ZoneOffset.UTC
import java.time.{Duration, Instant, OffsetDateTime, ZoneOffset, ZonedDateTime}

sealed trait Timing

object Timing {
  case class Timestamp private (value: Instant) extends Timing {
    def instant: Instant = value
  }

  object Timestamp {
    def apply(value: Instant): Timestamp =
      new Timestamp(Instant.ofEpochMilli(value.toEpochMilli))

    def apply(value: ZonedDateTime): Timestamp =
      apply(value.toInstant)

    given Ordering[Timestamp] =
      (x, y) => x.instant.compareTo(y.instant)
  }

  case class Timeframe private (value: Interval) extends Timing {

    def contains(timestamp: Timestamp): Boolean =
      interval.contains(timestamp.instant)

    def start: Timestamp =
      Timestamp(value = value.normalizedStart)

    def duration: Duration =
      value.normalizedDuration

    def end: Timestamp =
      Timestamp(value = value.normalizedStop)

    def interval: Interval =
      value.normalized
  }

  object Timeframe {
    def apply(value: Interval): Timeframe =
      new Timeframe(
        Interval(
          start = Instant.ofEpochMilli(value.normalizedStart.toEpochMilli),
          duration = Duration.ofMillis(value.normalizedDuration.toMillis)
        )
      )

    def apply(start: Timestamp, duration: Duration): Timeframe =
      apply(Interval(start.instant, duration))

    def apply(timestamps: NonEmptySeq[Timestamp]): Timeframe =
      apply(Interval(timestamps.map(_.value)))

    def apply(head: Timestamp, tail: Timestamp*): Timeframe =
      apply(NonEmptySeq(head, tail))
  }
}

package de.neuland.bandwhichd.server.lib.time

import _root_.cats.data.NonEmptySeq

import java.time.*
import java.time.ZoneOffset.UTC
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.util.Objects
import java.util.regex.{Matcher, Pattern}
import scala.util.{Failure, Success, Try}

case class Interval(start: Instant, duration: Duration) {
  def days: Iterator[LocalDate] = {
    val first = LocalDate.ofInstant(normalizedStart, UTC)
    val last = LocalDate.ofInstant(normalizedStop, UTC)
    Iterator.unfold[LocalDate, LocalDate](first)(current =>
      if (current.isAfter(last)) None
      else Some(current -> current.plusDays(1))
    )
  }

  def contains(instant: Instant): Boolean =
    !instant.isBefore(normalizedStart) && !instant.isAfter(normalizedStop)

  def normalizedStart: Instant =
    if (duration.isNegative)
      start.plus(duration)
    else
      start

  def normalizedDuration: Duration =
    duration.abs

  def normalizedStop: Instant =
    if (duration.isNegative)
      start
    else
      start.plus(duration)

  def normalized: Interval =
    if (duration.isNegative)
      new Interval(start.plus(duration), duration.abs)
    else
      this

  override def equals(obj: Any): Boolean =
    obj match
      case other: Interval =>
        normalizedStart == other.normalizedStart && normalizedDuration == other.normalizedDuration
      case _ => false

  override def hashCode: Int =
    normalizedStart.hashCode ^ normalizedDuration.hashCode

  override def toString: String =
    start.toString + "/" + duration.toString
}

object Interval {
  def apply(start: Instant, stop: Instant): Interval =
    Interval(start = start, duration = Duration.between(start, stop))

  def apply(instants: NonEmptySeq[Instant]): Interval =
    Interval(
      instants.iterator.min,
      instants.iterator.max
    )

  private val PATTERN = "([^/]+)/([^/]+)".r

  def parse(text: CharSequence): Try[Interval] =
    text match
      case PATTERN(startText, durationText) =>
        val start = Instant.parse(startText)
        val duration = Duration.parse(durationText)
        Success(Interval(start, duration))
      case _ =>
        Failure(
          new DateTimeParseException(
            "Text cannot be parsed to an Interval",
            text,
            0
          )
        )
}

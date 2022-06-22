package de.neuland.bandwhichd.server.lib.time

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{Duration, Instant, ZonedDateTime}
import java.util.Objects
import java.util.regex.{Matcher, Pattern}
import scala.util.{Failure, Success, Try}

case class Interval(start: Instant, duration: Duration) {
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

package de.neuland.bandwhichd.server.lib.time

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{Duration, ZonedDateTime}
import java.util.Objects
import java.util.regex.{Matcher, Pattern}
import scala.util.{Failure, Success, Try}

case class ZonedInterval(start: ZonedDateTime, duration: Duration) {
  def normalizedStart: ZonedDateTime =
    if (duration.isNegative)
      start.plus(duration)
    else
      start

  def normalizedDuration: Duration =
    duration.abs

  def normalizedStop: ZonedDateTime =
    if (duration.isNegative)
      start
    else
      start.plus(duration)

  def normalized: ZonedInterval =
    if (duration.isNegative)
      new ZonedInterval(start.plus(duration), duration.abs)
    else
      this

  override def equals(obj: Any): Boolean =
    obj match
      case other: ZonedInterval =>
        normalizedStart == other.normalizedStart && normalizedDuration == other.normalizedDuration
      case _ => false

  override def hashCode: Int =
    normalizedStart.hashCode ^ normalizedDuration.hashCode

  override def toString: String =
    start.toString + "/" + duration.toString
}

object ZonedInterval {
  private val PATTERN = "([^/]+)/([^/]+)".r

  def parse(text: CharSequence): Try[ZonedInterval] =
    parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME)

  def parse(
      text: CharSequence,
      startFormatter: DateTimeFormatter
  ): Try[ZonedInterval] =
    text match
      case PATTERN(startText, durationText) =>
        val start = ZonedDateTime.parse(startText, startFormatter)
        val duration = Duration.parse(durationText)
        Success(ZonedInterval(start, duration))
      case _ =>
        Failure(
          new DateTimeParseException(
            "Text cannot be parsed to an Interval",
            text,
            0
          )
        )
}

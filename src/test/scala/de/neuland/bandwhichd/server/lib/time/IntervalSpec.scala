package de.neuland.bandwhichd.server.lib.time

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Duration, Instant, LocalDate}

class IntervalSpec extends AnyWordSpec with Matchers {
  "Interval" when {
    "being inside a single day" should {
      "have that day" in {
        // given
        val interval = Interval(
          Instant.parse("2022-06-28T05:56:10.123Z"),
          Instant.parse("2022-06-28T23:07:12.456Z")
        )

        // when
        val result = interval.days

        // then
        result.toSeq should contain theSameElementsInOrderAs Seq(
          LocalDate.parse("2022-06-28")
        )
      }
    }

    "spanning multiple days" should {
      "have those days" in {
        // given
        val interval = Interval(
          Instant.parse("2022-06-28T23:59:59.999Z"),
          Instant.parse("2022-07-02T00:00:00.000Z")
        )

        // when
        val result = interval.days

        // then
        result.toSeq should contain theSameElementsInOrderAs Seq(
          LocalDate.parse("2022-06-28"),
          LocalDate.parse("2022-06-29"),
          LocalDate.parse("2022-06-30"),
          LocalDate.parse("2022-07-01"),
          LocalDate.parse("2022-07-02")
        )
      }
    }
  }
}

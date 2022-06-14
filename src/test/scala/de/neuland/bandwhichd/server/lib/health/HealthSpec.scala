package de.neuland.bandwhichd.server.lib.health

import io.circe.{Json, JsonObject}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HealthSpec extends AnyWordSpec with Matchers {
  "Health" should {
    "have status from single included check" when {
      "single included check has status pass" in {
        // given
        val check = createCheckWithStatus(Status.Pass)

        val health = Health(Seq(check))

        // when
        val result = health.status

        // then
        result shouldBe Status.Pass
      }

      "single included check has status warn" in {
        // given
        val check = createCheckWithStatus(Status.Warn)

        val health = Health(Seq(check))

        // when
        val result = health.status

        // then
        result shouldBe Status.Warn
      }

      "single included check has status fail" in {
        // given
        val check = createCheckWithStatus(Status.Fail)

        val health = Health(Seq(check))

        // when
        val result = health.status

        // then
        result shouldBe Status.Fail
      }
    }

    "have warn status" when {
      "built from non-standard checks" in {
        // given
        val check1 = createCheckWithCustomStatus("unknown")
        val check2 = createCheckWithoutStatus
        val check3 = createCheckWithCustomStatus("green")

        val health = Health(
          Seq(
            check1,
            check2,
            check3
          )
        )

        // when
        val result = health.status

        // then
        result shouldBe Status.Warn
      }
    }

    "have status from check with lowest status" when {
      "built from from multiple checks" in {
        // given
        val checkWithoutStatus = createCheckWithoutStatus
        val checkWithStatusPass = createCheckWithStatus(Status.Pass)
        val checkWithStatusWarn = createCheckWithStatus(Status.Fail)
        val checkWithCustomStatus = createCheckWithCustomStatus("unknown")

        val health = Health(
          Seq(
            checkWithStatusWarn,
            checkWithStatusPass,
            checkWithoutStatus,
            checkWithCustomStatus
          )
        )

        // when
        val result = health.status

        // then
        result shouldBe Status.Fail
      }
    }

    "have http response status code according to the status" when {
      "status is pass" in {
        // given
        val check = createCheckWithStatus(Status.Pass)

        val health = Health(Seq(check))

        // when
        val result = health.httpResponseStatusCode

        // then
        result shouldBe 200
      }

      "status is warn" in {
        // given
        val check = createCheckWithStatus(Status.Warn)

        val health = Health(Seq(check))

        // when
        val result = health.httpResponseStatusCode

        // then
        result shouldBe 290
      }

      "status is fail" in {
        // given
        val check = createCheckWithStatus(Status.Fail)

        val health = Health(Seq(check))

        // when
        val result = health.httpResponseStatusCode

        // then
        result shouldBe 503
      }
    }
  }

  private def createCheckWithStatus(status: Status) =
    createCheckWithCustomStatus(status.stringValue)

  private def createCheckWithCustomStatus(status: String) =
    new Check {
      override def key: CheckKey = ???
      override def value: JsonObject =
        JsonObject("status" -> Json.fromString(status))
    }

  private def createCheckWithoutStatus =
    new Check {
      override def key: CheckKey = ???
      override def value: JsonObject =
        JsonObject()
    }
}

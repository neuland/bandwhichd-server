package de.neuland.bandwhichd.server.adapter.in.v1.message

import de.neuland.bandwhichd.server.domain.measurement.MeasurementFixtures
import io.circe.{Encoder, Printer}
import io.circe.parser.decode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MessageSpec extends AnyWordSpec with Matchers {
  "Message" should {
    "be parse-able from API version 1 JSON for bandwhichd/measurement/agent-network-configuration/v1" in {
      // given
      val json =
        ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJson

      // when
      val result = decode[Message](json)

      // then
      result shouldBe Right(
        Message.MeasurementMessage(measurement =
          MeasurementFixtures.exampleNetworkConfigurationMeasurement
        )
      )
    }

    "be parse-able from API version 1 JSON for bandwhichd/measurement/agent-network-utilization/v1" in {
      // given
      val json = ApiV1MessageV1Fixtures.exampleNetworkUtilizationMeasurementJson

      // when
      val result = decode[Message](json)

      // then
      result shouldBe Right(
        Message.MeasurementMessage(measurement =
          MeasurementFixtures.exampleNetworkUtilizationMeasurement
        )
      )
    }

    "be writable if type is bandwhichd/measurement/agent-network-configuration/v1" in {
      // given
      val message = Message.MeasurementMessage(
        measurement = MeasurementFixtures.exampleNetworkConfigurationMeasurement
      )

      // when
      val result = print[Message](message)

      // then
      result shouldBe ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJsonNoSpaces
    }

    "be writable if type is bandwhichd/measurement/agent-network-utilization/v1" in {
      // given
      val message = Message.MeasurementMessage(
        measurement = MeasurementFixtures.exampleNetworkUtilizationMeasurement
      )

      // when
      val result = print[Message](message)

      // then
      result shouldBe ApiV1MessageV1Fixtures.exampleNetworkUtilizationMeasurementJsonNoSpaces
    }
  }

  private def print[A](a: A)(using Encoder[A]): String =
    Encoder[A].apply(a).noSpaces
}

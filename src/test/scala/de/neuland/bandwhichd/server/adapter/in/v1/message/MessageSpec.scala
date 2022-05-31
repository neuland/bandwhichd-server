package de.neuland.bandwhichd.server.adapter.in.v1.message

import de.neuland.bandwhichd.server.domain.measurement.MeasurementFixtures
import io.circe.parser.decode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MessageSpec extends AnyWordSpec with Matchers {
  "Message" should {
    "be parse-able from API version 1 JSON for bandwhichd/measurement/network-configuration/v1" in {
      // given
      val json =
        ApiV1MessageV1Fixtures.exampleNetworkConfigurationMeasurementJson

      // when
      val result = decode[Message](json)(Message.decoder)

      // then
      result shouldBe Right(
        Message.MeasurementMessage(measurement =
          MeasurementFixtures.exampleNetworkConfigurationMeasurement
        )
      )
    }

    "be parse-able from API version 1 JSON for bandwhichd/measurement/network-utilization/v1" in {
      // given
      val json = ApiV1MessageV1Fixtures.exampleNetworkUtilizationMeasurementJson

      // when
      val result = decode[Message](json)(Message.decoder)

      // then
      result shouldBe Right(
        Message.MeasurementMessage(measurement =
          MeasurementFixtures.exampleNetworkUtilizationMeasurement
        )
      )
    }
  }
}

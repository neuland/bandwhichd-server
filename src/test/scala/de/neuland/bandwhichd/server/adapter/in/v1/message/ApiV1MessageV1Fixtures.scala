package de.neuland.bandwhichd.server.adapter.in.v1.message

import scala.io.Source
import scala.util.Using

object ApiV1MessageV1Fixtures {
  def exampleNetworkConfigurationMeasurementJson: String =
    Using(
      Source.fromURL(
        getClass.getClassLoader.getResource(
          "de/neuland/bandwhichd/server/adapter/in/v1/message/bandwhichd/measurement/agent-network-configuration/v1/example.json"
        )
      )
    )(_.mkString).get

  def exampleNetworkConfigurationMeasurementJsonNoSpaces: String =
    io.circe.parser
      .parse(exampleNetworkConfigurationMeasurementJson)
      .toTry
      .get
      .noSpaces

  def exampleNetworkUtilizationMeasurementJson: String =
    Using(
      Source.fromURL(
        getClass.getClassLoader.getResource(
          "de/neuland/bandwhichd/server/adapter/in/v1/message/bandwhichd/measurement/agent-network-utilization/v1/example.json"
        )
      )
    )(_.mkString).get

  def exampleNetworkUtilizationMeasurementJsonNoSpaces: String =
    io.circe.parser
      .parse(exampleNetworkUtilizationMeasurementJson)
      .toTry
      .get
      .noSpaces
}

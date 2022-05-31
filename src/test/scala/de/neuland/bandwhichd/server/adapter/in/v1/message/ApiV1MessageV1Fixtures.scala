package de.neuland.bandwhichd.server.adapter.in.v1.message

import scala.io.Source
import scala.util.Using

object ApiV1MessageV1Fixtures {
  def exampleNetworkConfigurationMeasurementJson: String =
    Using(
      Source.fromURL(
        getClass.getClassLoader.getResource(
          "de/neuland/bandwhichd/server/adapter/in/v1/message/bandwhichd/measurement/network-configuration/v1/example.json"
        )
      )
    )(_.mkString).get

  def exampleNetworkUtilizationMeasurementJson: String =
    Using(
      Source.fromURL(
        getClass.getClassLoader.getResource(
          "de/neuland/bandwhichd/server/adapter/in/v1/message/bandwhichd/measurement/network-utilization/v1/example.json"
        )
      )
    )(_.mkString).get
}

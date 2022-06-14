package de.neuland.bandwhichd.server.lib.health

import io.circe.Json
import io.circe.JsonObject

trait Check {
  def key: CheckKey
  def value: JsonObject

  def maybeStatus: Option[Status] =
    value
      .apply("status")
      .flatMap(_.asString)
      .flatMap(Status.from)
}

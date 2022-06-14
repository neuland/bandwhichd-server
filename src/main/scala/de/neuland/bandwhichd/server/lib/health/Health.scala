package de.neuland.bandwhichd.server.lib.health

import io.circe.Json

case class Health(checks: Seq[Check]) {
  def status: Status =
    checks
      .flatMap(_.maybeStatus)
      .minOption
      .getOrElse(Status.Warn)

  def httpResponseStatusCode: Int =
    status match
      case Status.Pass => 200
      case Status.Warn => 290
      case Status.Fail => 503

  def asJson: Json =
    Json.obj(
      "status" -> Json.fromString(status.stringValue),
      "checks" -> Json.fromFields(
        checks
          .groupBy(_.key)
          .map { case (checkKey, checks) =>
            checkKey.value -> Json.fromValues(
              checks
                .map(_.value)
                .map(Json.fromJsonObject)
            )
          }
      )
    )
}

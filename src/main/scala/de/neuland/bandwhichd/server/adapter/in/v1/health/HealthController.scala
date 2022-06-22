package de.neuland.bandwhichd.server.adapter.in.v1.health

import cats.effect.Async
import de.neuland.bandwhichd.server.lib.health.jvm.JvmMemoryUtilization
import de.neuland.bandwhichd.server.lib.health.{Check, Health}
import fs2.Pure
import io.circe.Json
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.{Entity, HttpRoutes, Response, Status}

class HealthController[F[_]: Async] extends Http4sDsl[F] {

  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "v1" / "health" =>
      health
    }

  private val currentHealth = Health(
    Seq[Check](
      JvmMemoryUtilization.current
    )
  )

  private def health: F[Response[F]] = {
    val encoder = org.http4s.circe.jsonEncoder

    val status: Status =
      Status
        .fromInt(currentHealth.httpResponseStatusCode)
        .getOrElse(Status.InternalServerError)
    val entity: Entity[Pure] =
      encoder.toEntity(currentHealth.asJson)

    Async[F].pure(
      Response(
        status = status,
        headers = encoder.headers,
        entity = entity
      )
    )
  }
}

package de.neuland.bandwhichd.server.adapter.in.v1.health

import cats.effect.Async
import io.circe.Json
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.middleware.CORS
import org.http4s.{HttpRoutes, Response}

class HealthController[F[_]: Async] extends Http4sDsl[F] {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "v1" / "health" =>
      health
    }

  private def health: F[Response[F]] = {
    import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
    val healthStatus: Json =
      Json.obj(
        "status" -> Json.fromString("pass")
      )
    Ok(healthStatus)
  }
}

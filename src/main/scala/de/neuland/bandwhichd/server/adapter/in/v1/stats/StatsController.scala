package de.neuland.bandwhichd.server.adapter.in.v1.stats

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.*
import de.neuland.bandwhichd.server.application.StatsApplicationService
import de.neuland.bandwhichd.server.domain.measurement.Timing
import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.http4s.Helpers
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import io.circe.Json
import io.circe.syntax.*
import org.http4s.*
import org.http4s.dsl.{io as _, *}
import org.http4s.headers.*
import org.http4s.implicits.*

import java.time.OffsetDateTime

class StatsController[F[_]: Async](
    private val statsApplicationService: StatsApplicationService[F]
) extends Http4sDsl[F],
      Helpers {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case request @ GET -> Root / "v1" / "stats" :? From(from) +& To(to) =>
        validTuple2OrBadRequest(get(request))(from -> to)
    }

  private def get(
      request: Request[F]
  )(
      fromTo: (Option[OffsetDateTime], Option[OffsetDateTime])
  ): F[Response[F]] =
    for {
      maybeTimeframe <- deriveMaybeTimeframe(fromTo)
      monitoredStats <- statsApplicationService.get(maybeTimeframe)
      response <- {
        val statsWithinMonitoredNetworks: MonitoredStats =
          monitoredStats.withoutHostsOutsideOfMonitoredNetworks
        import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
        val json: Json =
          statsWithinMonitoredNetworks.asJson(StatsCodecs.encoder)
        Ok(json)
      }
    } yield response
}

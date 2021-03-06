package de.neuland.bandwhichd.server.adapter.in.v1.stats

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.*
import de.neuland.bandwhichd.server.application.StatsApplicationService
import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.dot.Dot
import de.neuland.bandwhichd.server.lib.http4s.Helpers
import de.neuland.bandwhichd.server.lib.http4s.dot.DotHttp4s
import io.circe.syntax.*
import io.circe.Json
import org.http4s.dsl.{io as _, *}
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.*

class StatsController[F[_]: Async](
    private val statsApplicationService: StatsApplicationService[F]
) extends Http4sDsl[F],
      Helpers {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] { case request @ GET -> Root / "v1" / "stats" =>
      stats(request)
    }

  private def stats(request: Request[F]): F[Response[F]] =
    for {
      monitoredStats: MonitoredStats <- statsApplicationService.get
      response <- {
        val statsWithinMonitoredNetworks: MonitoredStats =
          monitoredStats.withoutHostsOutsideOfMonitoredNetworks
        if (useDotInsteadOfJson(request.headers)) {
          import de.neuland.bandwhichd.server.lib.http4s.dot.DotHttp4s.dotEntityEncoder
          val dot: Dot = StatsCodecs.dotEncoder(statsWithinMonitoredNetworks)
          Ok(dot)
        } else {
          import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
          val json: Json =
            statsWithinMonitoredNetworks.asJson(StatsCodecs.circeEncoder)
          Ok(json)
        }
      }
    } yield response

  private def useDotInsteadOfJson(headers: Headers) = {
    val maybeAcceptHeader: Option[Accept] =
      headers.get(
        Header.Select.recurringHeadersWithMerge(
          org.http4s.headers.Accept.headerSemigroupInstance,
          org.http4s.headers.Accept.headerInstance
        )
      )

    maybeAcceptHeader.fold(false) { acceptHeader =>

      val mediaRangeAndQValues: NonEmptyList[MediaRangeAndQValue] =
        acceptHeader.values

      val maybeDotMediaRangeAndQValue: Option[MediaRangeAndQValue] =
        mediaRangeAndQValues.find { mediaRangeAndQValue =>
          DotHttp4s.mediaType.satisfiedBy(mediaRangeAndQValue.mediaRange)
        }

      maybeDotMediaRangeAndQValue.fold(false) { dotMediaRangeAndQValue =>

        val maybeJsonMediaRangeAndQValue =
          mediaRangeAndQValues.find { mediaRangeAndQValue =>
            MediaType.application.json.satisfiedBy(
              mediaRangeAndQValue.mediaRange
            )
          }

        maybeJsonMediaRangeAndQValue.fold(true) { jsonMediaRangeAndQValue =>
          dotMediaRangeAndQValue.qValue.compare(
            jsonMediaRangeAndQValue.qValue
          ) >= 0
        }
      }
    }
  }
}

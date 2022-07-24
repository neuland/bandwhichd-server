package de.neuland.bandwhichd.server.adapter.in.v1.message

import cats.data.Validated.{Invalid, Valid}
import cats.effect.{Concurrent, Resource}
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.v1.message.Message.MeasurementMessage
import de.neuland.bandwhichd.server.application.MeasurementApplicationService
import de.neuland.bandwhichd.server.boot.Configuration
import de.neuland.bandwhichd.server.domain.measurement.Timing
import de.neuland.bandwhichd.server.domain.stats.Stats
import de.neuland.bandwhichd.server.lib.http4s.Helpers
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import io.circe.{Decoder, Encoder, Json}
import org.http4s.dsl.{io as _, *}
import org.http4s.headers.Allow
import org.http4s.implicits.*
import org.http4s.{Message as _, *}

import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime}

class MessageController[F[_]: Concurrent](
    private val configuration: Configuration,
    private val timeContext: TimeContext[F],
    private val measurementApplicationService: MeasurementApplicationService[F]
) extends Http4sDsl[F],
      Helpers {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "v1" / "messages" :? From(from) +& To(to) =>
        validTuple2OrBadRequest(get)(from -> to)
      case request @ POST -> Root / "v1" / "messages" => publish(request)
    }

  private val circeInstances = org.http4s.circe.CirceInstances.builder.build

  private def get(
      fromTo: (Option[OffsetDateTime], Option[OffsetDateTime])
  ): F[Response[F]] =
    for {
      timeframe <- deriveTimeframe(timeContext)(fromTo)
    } yield {
      val encoder = circeInstances.streamJsonArrayEncoder[F]
      Response(
        entity = encoder.toEntity(
          measurementApplicationService
            .get(timeframe)
            .map(Message.apply)
            .map(Encoder[Message].apply)
        ),
        headers = encoder.headers
      )
    }

  private def publish(request: Request[F]): F[Response[F]] =
    if (configuration.readonly)
      MethodNotAllowed(Allow())
    else
      fromJsonOrBadRequest[Message, F](request)(message =>
        for {
          _ <- {
            message match
              case Message.MeasurementMessage(measurement) =>
                measurementApplicationService.record(measurement)
          }
          response <- Ok("")
        } yield response
      )
}

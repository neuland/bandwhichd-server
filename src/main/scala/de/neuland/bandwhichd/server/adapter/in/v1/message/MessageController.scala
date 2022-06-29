package de.neuland.bandwhichd.server.adapter.in.v1.message

import io.circe.Encoder
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNel
import cats.effect.{Concurrent, Resource}
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.v1.message.Message.MeasurementMessage
import de.neuland.bandwhichd.server.application.{
  MeasurementApplicationService,
  RecordMeasurementCommand
}
import de.neuland.bandwhichd.server.boot.Configuration
import de.neuland.bandwhichd.server.domain.measurement.Timing
import de.neuland.bandwhichd.server.domain.stats.Stats
import de.neuland.bandwhichd.server.lib.http4s.Helpers
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import io.circe.{Decoder, Json}
import org.http4s.dsl.{io as _, *}
import org.http4s.headers.Allow
import org.http4s.implicits.*
import org.http4s.{Message as _, *}

import java.time.{Instant, OffsetDateTime}
import java.time.format.DateTimeFormatter

class MessageController[F[_]: Concurrent](
    private val configuration: Configuration,
    private val timeContext: TimeContext[F],
    private val measurementApplicationService: MeasurementApplicationService[F]
) extends Http4sDsl[F],
      Helpers {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "v1" / "messages" :? From(from) +& To(to) =>
        get(from, to)
      case request @ POST -> Root / "v1" / "messages" => publish(request)
    }

  private type OptionalValidatedParam[A] = Option[ValidatedNel[ParseFailure, A]]

  private given QueryParamCodec[OffsetDateTime] =
    QueryParamCodec.offsetDateTime(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  private object From
      extends OptionalValidatingQueryParamDecoderMatcher[OffsetDateTime]("from")

  private object To
      extends OptionalValidatingQueryParamDecoderMatcher[OffsetDateTime]("to")

  private val circeInstances = org.http4s.circe.CirceInstances.builder.build

  private def get(
      from: OptionalValidatedParam[OffsetDateTime],
      to: OptionalValidatedParam[OffsetDateTime]
  ): F[Response[F]] =
    (from, to) match
      case (Some(Invalid(_)), Some(Invalid(_))) => BadRequest("")
      case (Some(Invalid(_)), _)                => BadRequest("")
      case (_, Some(Invalid(_)))                => BadRequest("")
      case (Some(Valid(from)), Some(Valid(to))) =>
        getValid(Some(from), Some(to))
      case (Some(Valid(from)), _) => getValid(Some(from), None)
      case (_, Some(Valid(to)))   => getValid(None, Some(to))
      case _                      => getValid(None, None)

  private def getValid(
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): F[Response[F]] =
    for {
      timeframe <- timeframe(from, to)
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
                measurementApplicationService.recordMeasurement(
                  RecordMeasurementCommand(measurement)
                )
          }
          response <- Ok("")
        } yield response
      )

  private def timeframe(
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): F[Timing.Timeframe] =
    (from, to) match
      case (Some(from), Some(to)) =>
        Concurrent[F].pure(
          Timing.Timeframe(Interval(from.toInstant, to.toInstant))
        )
      case (Some(from), None) =>
        Concurrent[F].pure(
          Timing.Timeframe(
            Interval(from.toInstant, Stats.defaultTimeframeDuration)
          )
        )
      case (None, Some(to)) =>
        Concurrent[F].pure(
          Timing.Timeframe(
            Interval(
              to.toInstant.minus(Stats.defaultTimeframeDuration),
              Stats.defaultTimeframeDuration
            )
          )
        )
      case (None, None) => Stats.defaultTimeframe(timeContext)
}

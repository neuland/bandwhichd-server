package de.neuland.bandwhichd.server.lib.http4s

import cats.Monad
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import cats.effect.Concurrent
import cats.implicits.*
import de.neuland.bandwhichd.server.domain.measurement.Timing
import de.neuland.bandwhichd.server.domain.stats.Stats
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import io.circe.Decoder
import org.http4s.*
import org.http4s.dsl.*

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

trait Helpers extends RequestDsl {
  type OptionalValidatedParam[A] = Option[ValidatedNel[ParseFailure, A]]

  given QueryParamCodec[OffsetDateTime] =
    QueryParamCodec.offsetDateTime(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  object From
      extends OptionalValidatingQueryParamDecoderMatcher[OffsetDateTime]("from")

  object To
      extends OptionalValidatingQueryParamDecoderMatcher[OffsetDateTime]("to")

  def deriveTimeframe[F[_]: Monad](
      timeContext: TimeContext[F]
  )(
      fromTo: (Option[OffsetDateTime], Option[OffsetDateTime])
  ): F[Timing.Timeframe] =
    fromTo match
      case (Some(from), Some(to)) =>
        Monad[F].pure(
          Timing.Timeframe(Interval(from.toInstant, to.toInstant))
        )
      case (Some(from), None) =>
        Monad[F].pure(
          Timing.Timeframe(
            Interval(from.toInstant, Stats.defaultTimeframeDuration)
          )
        )
      case (None, Some(to)) =>
        Monad[F].pure(
          Timing.Timeframe(
            Interval(
              to.toInstant.minus(Stats.defaultTimeframeDuration),
              Stats.defaultTimeframeDuration
            )
          )
        )
      case (None, None) =>
        for {
          now <- timeContext.now
        } yield Timing.Timeframe(
          Interval(now.minus(Stats.defaultTimeframeDuration), now)
        )

  def deriveMaybeTimeframe[F[_]: Monad](
      fromTo: (Option[OffsetDateTime], Option[OffsetDateTime])
  ): F[Option[Timing.Timeframe]] =
    fromTo match
      case (Some(from), Some(to)) =>
        Monad[F].pure(
          Some(
            Timing.Timeframe(Interval(from.toInstant, to.toInstant))
          )
        )
      case (Some(from), None) =>
        Monad[F].pure(
          Some(
            Timing.Timeframe(
              Interval(from.toInstant, Stats.defaultTimeframeDuration)
            )
          )
        )
      case (None, Some(to)) =>
        Monad[F].pure(
          Some(
            Timing.Timeframe(
              Interval(
                to.toInstant.minus(Stats.defaultTimeframeDuration),
                Stats.defaultTimeframeDuration
              )
            )
          )
        )
      case (None, None) =>
        Monad[F].pure(None)

  def validTuple2OrBadRequest[
      A,
      B,
      F[_]: Monad
  ](
      f: ((Option[A], Option[B])) => F[Response[F]]
  )(
      t: (OptionalValidatedParam[A], OptionalValidatedParam[B])
  ): F[Response[F]] =
    t match
      case (Some(Invalid(_)), Some(Invalid(_))) => badRequest
      case (Some(Invalid(_)), _)                => badRequest
      case (_, Some(Invalid(_)))                => badRequest
      case (Some(Valid(_1)), Some(Valid(_2)))   => f(Some(_1), Some(_2))
      case (Some(Valid(_1)), _)                 => f(Some(_1), None)
      case (_, Some(Valid(_2)))                 => f(None, Some(_2))
      case _                                    => f(None, None)

  def fromJsonOrBadRequest[A, F[_]: Concurrent](request: Request[F])(
      valid: A => F[Response[F]]
  )(using Decoder[A]): F[Response[F]] =
    fromJson(request)(valid)(_ => badRequest)

  def fromJson[A, F[_]: Concurrent](request: Request[F])(
      valid: A => F[Response[F]]
  )(
      invalid: DecodeFailure => F[Response[F]]
  )(using Decoder[A]): F[Response[F]] = {
    import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
    val eventualBody: DecodeResult[F, A] = request.attemptAs[A]
    for {
      maybeBody: Either[DecodeFailure, A] <- eventualBody.value
      response <- maybeBody match
        case Left(decodeFailure) => invalid.apply(decodeFailure)
        case Right(a)            => valid.apply(a)
    } yield response
  }

  def badRequest[F[_]: Monad]: F[Response[F]] =
    Monad[F].pure(Response(status = Status.BadRequest))
}

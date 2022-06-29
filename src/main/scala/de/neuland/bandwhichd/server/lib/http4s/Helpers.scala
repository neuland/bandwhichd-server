package de.neuland.bandwhichd.server.lib.http4s

import cats.effect.Concurrent
import cats.implicits.*
import io.circe.Decoder
import org.http4s.*

trait Helpers {
  def fromJsonOrBadRequest[A, F[_]: Concurrent](request: Request[F])(
      valid: A => F[Response[F]]
  )(using Decoder[A]): F[Response[F]] =
    fromJson(request)(valid)(_ =>
      Concurrent[F].pure(Response.apply(status = Status.BadRequest))
    )

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
}

object Helpers extends Helpers

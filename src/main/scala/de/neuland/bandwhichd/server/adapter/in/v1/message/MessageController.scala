package de.neuland.bandwhichd.server.adapter.in.v1.message

import cats.effect.Async
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.v1.message.Message.MeasurementMessage
import de.neuland.bandwhichd.server.application.{
  MeasurementApplicationService,
  RecordMeasurementCommand
}
import de.neuland.bandwhichd.server.boot.Configuration
import de.neuland.bandwhichd.server.lib.http4s.Helpers
import io.circe.Decoder
import org.http4s.dsl.{io as _, *}
import org.http4s.headers.Allow
import org.http4s.implicits.*
import org.http4s.{Message as _, *}

class MessageController[F[_]: Async](
    private val configuration: Configuration,
    private val measurementApplicationService: MeasurementApplicationService[F]
) extends Http4sDsl[F],
      Helpers {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] { case request @ POST -> Root / "v1" / "message" =>
      publish(request)
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
      )(implicitly, Message.decoder)
}

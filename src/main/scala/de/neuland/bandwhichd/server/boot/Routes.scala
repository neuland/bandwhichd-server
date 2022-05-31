package de.neuland.bandwhichd.server.boot

import cats.effect.Async
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.v1.message.MessageController
import de.neuland.bandwhichd.server.adapter.in.v1.stats.StatsController
import org.http4s.HttpRoutes

class Routes[F[_]: Async](
    private val messageController: MessageController[F],
    private val statsController: StatsController[F]
) {
  val routes: HttpRoutes[F] =
    messageController.routes <+> statsController.routes
}

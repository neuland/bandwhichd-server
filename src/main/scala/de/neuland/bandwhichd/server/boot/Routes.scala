package de.neuland.bandwhichd.server.boot

import cats.effect.Async
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.v1.health.HealthController
import de.neuland.bandwhichd.server.adapter.in.v1.message.MessageController
import de.neuland.bandwhichd.server.adapter.in.v1.stats.StatsController
import org.http4s.HttpRoutes
import org.http4s.server.middleware.CORS

class Routes[F[_]: Async](
    private val healthController: HealthController[F],
    private val messageController: MessageController[F],
    private val statsController: StatsController[F]
) {
  val routes: HttpRoutes[F] =
    CORS.policy.withAllowOriginAll(
      healthController.routes
        <+> messageController.routes
        <+> statsController.routes
    )
}

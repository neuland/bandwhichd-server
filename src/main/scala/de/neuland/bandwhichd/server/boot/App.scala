package de.neuland.bandwhichd.server.boot

import cats.effect.*
import cats.effect.kernel.Outcome
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.scheduler.AggregationScheduler
import de.neuland.bandwhichd.server.adapter.in.v1.message.MessageController
import de.neuland.bandwhichd.server.adapter.in.v1.stats.StatsController
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementInMemoryRepository
import de.neuland.bandwhichd.server.adapter.out.stats.StatsInMemoryRepository
import de.neuland.bandwhichd.server.application.{
  MeasurementApplicationService,
  StatsApplicationService
}
import de.neuland.bandwhichd.server.domain.measurement.MeasurementRepository
import de.neuland.bandwhichd.server.domain.stats.StatsRepository
import de.neuland.bandwhichd.server.lib.scheduling.{
  Operator,
  Scheduler,
  SchedulersOperator
}
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.{HttpApp, HttpRoutes}

import scala.io.StdIn

class App[F[_]: Async] {
  // out
  val measurementRepository: MeasurementRepository[F] =
    MeasurementInMemoryRepository[F]()
  val statsRepository: StatsRepository[F] =
    StatsInMemoryRepository[F]()

  // application
  val measurementApplicationService: MeasurementApplicationService[F] =
    MeasurementApplicationService[F](
      measurementRepository = measurementRepository
    )

  val statsApplicationService: StatsApplicationService[F] =
    StatsApplicationService[F](
      measurementRepository = measurementRepository,
      statsRepository = statsRepository
    )

  // in http
  val messageController: MessageController[F] =
    MessageController[F](
      measurementApplicationService = measurementApplicationService
    )
  val statsController: StatsController[F] =
    StatsController[F](
      statsApplicationService = statsApplicationService
    )

  // in scheduling
  val aggregationScheduler: Scheduler[F] =
    AggregationScheduler[F](
      statsApplicationService = statsApplicationService
    )

  // http
  val routes: Routes[F] =
    Routes[F](
      messageController = messageController,
      statsController = statsController
    )
  // org.http4s.syntax.KleisliResponseOps#orNotFound
  val httpApp: HttpApp[F] =
    routes.routes.orNotFound

  // scheduling
  val schedulersOperator: Operator[F] =
    SchedulersOperator[F](
      aggregationScheduler
    )
}

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val main = App[IO]()

    val serverResource: Resource[IO, Server] =
      EmberServerBuilder
        .default[IO]
        .withHostOption(None)
        .withHttpApp(main.httpApp)
        .build

    val schedulersResource: Resource[IO, IO[Outcome[IO, Throwable, Unit]]] =
      main.schedulersOperator.resource

    val resources = for {
      schedulerOutcomeF <- schedulersResource
      server <- serverResource
    } yield (schedulerOutcomeF, server)

    val schedulerOutcomeF = resources
      .use { case (schedulerOutcomeF, _) =>
        schedulerOutcomeF
      }

    for {
      schedulerOutcome <- schedulerOutcomeF
    } yield {
      schedulerOutcome match
        case Outcome.Succeeded(_) =>
          ExitCode.Success
        case Outcome.Errored(throwable) =>
          throwable.printStackTrace()
          ExitCode.Error
        case Outcome.Canceled() =>
          ExitCode.Error
    }
  }
}

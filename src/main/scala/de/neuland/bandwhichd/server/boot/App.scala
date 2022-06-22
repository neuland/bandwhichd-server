package de.neuland.bandwhichd.server.boot

import cats.effect.*
import cats.effect.kernel.Outcome
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.scheduler.AggregationScheduler
import de.neuland.bandwhichd.server.adapter.in.v1.health.HealthController
import de.neuland.bandwhichd.server.adapter.in.v1.message.MessageController
import de.neuland.bandwhichd.server.adapter.in.v1.stats.StatsController
import de.neuland.bandwhichd.server.adapter.out.CassandraMigration
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementCassandraRepository
import de.neuland.bandwhichd.server.adapter.out.stats.StatsInMemoryRepository
import de.neuland.bandwhichd.server.application.{
  MeasurementApplicationService,
  StatsApplicationService
}
import de.neuland.bandwhichd.server.domain.measurement.MeasurementRepository
import de.neuland.bandwhichd.server.domain.stats.StatsRepository
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext
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

class App[F[_]: Async](
    private val cassandraContext: CassandraContext[F],
    private val configuration: Configuration
) {
  // out
  val measurementCassandraRepository: MeasurementCassandraRepository[F] =
    MeasurementCassandraRepository[F](
      cassandraContext = cassandraContext,
      configuration = configuration
    )
  val measurementRepository: MeasurementRepository[F] =
    measurementCassandraRepository
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
  val healthController: HealthController[F] =
    HealthController[F]()
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
      healthController = healthController,
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
    val schedulerOutcomeR = for {
      configuration <- Configuration.resource[IO]
      cassandraContext <- CassandraContext.resource[IO](configuration)
      _ <- Resource.eval(
        CassandraMigration(cassandraContext).migrate(configuration)
      )
      main = App[IO](cassandraContext, configuration)
      schedulerOutcomeF <- main.schedulersOperator.resource
      _ <- EmberServerBuilder
        .default[IO]
        .withHostOption(None)
        .withHttpApp(main.httpApp)
        .build
    } yield schedulerOutcomeF

    for {
      schedulerOutcome <- schedulerOutcomeR.use(identity)
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

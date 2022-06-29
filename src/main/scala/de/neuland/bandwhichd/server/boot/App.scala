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
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.{HttpApp, HttpRoutes}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

import java.io.{BufferedReader, InputStreamReader}
import java.util.Scanner
import scala.io.StdIn

class App[F[_]: Async](
    private val timeContext: TimeContext[F],
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
      timeContext = timeContext,
      measurementRepository = measurementRepository,
      statsRepository = statsRepository
    )

  // in scheduling
  val aggregationScheduler: Scheduler[F] =
    AggregationScheduler[F](
      configuration = configuration,
      statsApplicationService = statsApplicationService
    )

  // scheduling
  val schedulersOperator: SchedulersOperator[F] =
    SchedulersOperator[F](
      aggregationScheduler
    )

  // in http
  val healthController: HealthController[F] =
    HealthController[F]()
  val messageController: MessageController[F] =
    MessageController[F](
      configuration = configuration,
      timeContext = timeContext,
      measurementApplicationService = measurementApplicationService
    )
  val statsController: StatsController[F] =
    StatsController[F](
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
}

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val outcomeR = for {
      configuration <- Configuration.resource[IO]
      cassandraContext <- CassandraContext.resource[IO](configuration)
      _ <- Resource.eval(
        CassandraMigration(cassandraContext).migrate(configuration)
      )
      main = App[IO](
        TimeContext.systemTimeContext,
        cassandraContext,
        configuration
      )
      schedulerOutcomeF <- main.schedulersOperator.resource
      server <- EmberServerBuilder
        .default[IO]
        .withHostOption(None)
        .withHttpApp(main.httpApp)
        .build
      logger <- Resource.eval(Slf4jLogger.create[IO])
      _ <- Resource.eval(
        logger.info(
          s"bandwhichd-server startup complete - ${main.schedulersOperator.size} scheduler - listening on ${server.address}"
        )
      )
      lineF <- Resource.eval(IO.delay {
        for {
          line <- IO.interruptible {
            StdIn.readLine()
          }
          _ <- if (line == null) IO.never else IO.unit
        } yield ()
      })
    } yield schedulerOutcomeF.race(lineF)

    outcomeR.use { outcomeF =>
      for {
        outcome <- outcomeF
      } yield outcome match
        case Right(_)                   => ExitCode.Success
        case Left(Outcome.Succeeded(_)) => ExitCode.Success
        case _                          => ExitCode.Error
    }
  }
}

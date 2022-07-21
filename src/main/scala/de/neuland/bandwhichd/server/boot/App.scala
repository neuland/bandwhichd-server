package de.neuland.bandwhichd.server.boot

import cats.effect.*
import cats.effect.kernel.Outcome
import cats.implicits.*
import de.neuland.bandwhichd.server.adapter.in.v1.health.HealthController
import de.neuland.bandwhichd.server.adapter.in.v1.message.MessageController
import de.neuland.bandwhichd.server.adapter.in.v1.stats.StatsController
import de.neuland.bandwhichd.server.adapter.out.CassandraMigration
import de.neuland.bandwhichd.server.adapter.out.measurement.MeasurementsCassandraRepository
import de.neuland.bandwhichd.server.adapter.out.stats.StatsInMemoryRepository
import de.neuland.bandwhichd.server.application.{
  MeasurementApplicationService,
  StatsApplicationService
}
import de.neuland.bandwhichd.server.domain.measurement.MeasurementsRepository
import de.neuland.bandwhichd.server.domain.stats.StatsRepository
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext
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

open class App[F[_]: Async](
    private val timeContext: TimeContext[F],
    private val cassandraContext: CassandraContext[F],
    private val configuration: Configuration
) {
  // out
  lazy val measurementCassandraRepository: MeasurementsCassandraRepository[F] =
    MeasurementsCassandraRepository[F](
      cassandraContext = cassandraContext,
      configuration = configuration
    )
  lazy val measurementsRepository: MeasurementsRepository[F] =
    measurementCassandraRepository
  lazy val statsRepository: StatsRepository[F] =
    StatsInMemoryRepository[F]()

  // application
  lazy val measurementApplicationService: MeasurementApplicationService[F] =
    MeasurementApplicationService[F](
      measurementsRepository = measurementsRepository,
      statsRepository = statsRepository,
      timeContext = timeContext
    )

  lazy val statsApplicationService: StatsApplicationService[F] =
    StatsApplicationService[F](
      statsRepository = statsRepository
    )

  // in http
  lazy val healthController: HealthController[F] =
    HealthController[F]()
  lazy val messageController: MessageController[F] =
    MessageController[F](
      configuration = configuration,
      timeContext = timeContext,
      measurementApplicationService = measurementApplicationService
    )
  lazy val statsController: StatsController[F] =
    StatsController[F](
      statsApplicationService = statsApplicationService
    )

  // http
  lazy val routes: Routes[F] =
    Routes[F](
      healthController = healthController,
      messageController = messageController,
      statsController = statsController
    )
  // org.http4s.syntax.KleisliResponseOps#orNotFound
  lazy val httpApp: HttpApp[F] =
    routes.routes.orNotFound
}

object App extends IOApp.Simple {
  override def run: IO[Unit] =
    (for {
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
      server <- EmberServerBuilder
        .default[IO]
        .withHostOption(None)
        .withHttpApp(main.httpApp)
        .build
    } yield server).use { server =>
      for {
        logger <- Slf4jLogger.create[IO]
        _ <- logger.info(
          s"bandwhichd-server startup complete - listening on ${server.address}"
        )
        line <- IO.interruptible { StdIn.readLine() }
        _ <- if (line == null) IO.never else IO.unit
      } yield ()
    }
}

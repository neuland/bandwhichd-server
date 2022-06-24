package de.neuland.bandwhichd.server.lib.scheduling

import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Failure, Success, Try}

class SchedulerOperator[F[_]: Async](
    private val scheduler: Scheduler[F]
) extends Operator[F] {
  override def resource: Resource[F, F[Outcome[F, Throwable, Unit]]] =
    for {
      logger <- Resource.eval(Slf4jLogger.create[F])
      schedule <- Resource.eval(Async[F].defer(scheduler.schedule))
      outcome: F[Outcome[F, Throwable, Unit]] <- Async[F].background {
        schedule match
          case Schedule.Pausing(name, pauseDuration, work) =>
            def cycle: F[Unit] =
              for {
                _ <- logger.debug(s"Running $name")
                _ <- Async[F].onError(work.run) { case e =>
                  logger.error(e)(s"Scheduler $name failed")
                }
                _ <- logger.debug(s"Pausing $name for $pauseDuration")
                _ <- Async[F].sleep(pauseDuration)
                _ <- cycle
              } yield ()
            cycle
      }
    } yield outcome
}

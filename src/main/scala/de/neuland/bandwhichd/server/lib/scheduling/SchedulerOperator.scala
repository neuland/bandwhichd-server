package de.neuland.bandwhichd.server.lib.scheduling

import cats.effect.*
import cats.implicits.*

import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Failure, Success, Try}

class SchedulerOperator[F[_]: Async](
    private val scheduler: Scheduler[F]
) extends Operator[F] {
  override def resource: Resource[F, F[Outcome[F, Throwable, Unit]]] =
    for {
      schedule <- Resource.make(
        Async[F].defer(scheduler.schedule)
      )(_ => Async[F].pure(()))
      outcome: F[Outcome[F, Throwable, Unit]] <- Async[F].background {
        schedule match
          case Schedule.Pausing(pauseDuration, work) =>
            def cycle: F[Unit] =
              for {
                _ <- work.run
                _ <- Async[F].sleep(pauseDuration)
                _ <- cycle
              } yield ()
            cycle
      }
    } yield outcome
}

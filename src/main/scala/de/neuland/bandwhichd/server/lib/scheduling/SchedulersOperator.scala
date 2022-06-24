package de.neuland.bandwhichd.server.lib.scheduling

import cats.effect.{Async, Outcome, Resource}
import cats.implicits.*

class SchedulersOperator[F[_]: Async](private val schedulers: Scheduler[F]*)
    extends Operator[F] {
  def size: Int = schedulers.size

  override def resource: Resource[F, F[Outcome[F, Throwable, Unit]]] =
    schedulers
      .map(scheduler => SchedulerOperator(scheduler))
      .map(schedulerOperator => schedulerOperator.resource)
      .foldLeft[Resource[F, F[Outcome[F, Throwable, Unit]]]](
        Resource.make(
          Async[F].pure(
            Async[F].pure(
              Outcome.succeeded(
                Async[F].pure(())
              )
            )
          )
        )(_ => Async[F].pure(()))
      ) { case (accR, curR) =>
        for {
          accF <- accR
          curF <- curR
        } yield {
          for {
            accAndCur: (
                Outcome[F, Throwable, Unit],
                Outcome[F, Throwable, Unit]
            ) <- Async[F].both(accF, curF)
          } yield {
            if (accAndCur._1.isError)
              accAndCur._1
            else
              accAndCur._2
          }
        }
      }
}

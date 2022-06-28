package de.neuland.bandwhichd.server.lib.time.cats

import cats.Monad

import java.time.Instant

object TimeContextMocks {
  def unimplementedTimeContext[F[_]: Monad]: TimeContext[F] =
    new TimeContext[F] {
      override def now: F[Instant] = ???
    }

  def fixedTimeContext[F[_]: Monad](fixedNow: Instant): TimeContext[F] =
    new TimeContext[F] {
      override def now: F[Instant] = Monad[F].pure(fixedNow)
    }
}

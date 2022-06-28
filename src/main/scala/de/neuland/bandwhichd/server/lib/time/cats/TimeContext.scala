package de.neuland.bandwhichd.server.lib.time.cats

import cats.effect.Sync

import java.time.Instant

trait TimeContext[F[_]] {
  def now: F[Instant]
}

object TimeContext {
  def systemTimeContext[F[_]: Sync]: TimeContext[F] =
    new TimeContext[F] {
      override def now: F[Instant] = Sync[F].delay(Instant.now())
    }
}

package de.neuland.bandwhichd.server.adapter.out.stats

import cats.Monad
import cats.effect.Sync
import de.neuland.bandwhichd.server.domain.stats.{Stats, StatsRepository}

import java.util.concurrent.atomic.AtomicReference

class StatsInMemoryRepository[F[_]: Sync] extends StatsRepository[F] {
  private val statsStore: AtomicReference[Stats] =
    new AtomicReference[Stats](Stats.empty)

  override def safe(stats: Stats): F[Unit] =
    Sync[F].blocking {
      statsStore.set(stats)
    }

  override def get: F[Stats] =
    Sync[F].blocking {
      statsStore.get()
    }
}

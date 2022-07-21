package de.neuland.bandwhichd.server.adapter.out.stats

import cats.Monad
import cats.effect.Sync
import de.neuland.bandwhichd.server.domain.stats.*

import java.util.concurrent.atomic.AtomicReference

class StatsInMemoryRepository[F[_]: Sync] extends StatsRepository[F] {
  private val statsStore: AtomicReference[MonitoredStats] =
    new AtomicReference[MonitoredStats](Stats.empty)

  override def safe(stats: MonitoredStats): F[Unit] =
    Sync[F].blocking {
      statsStore.set(stats)
    }

  override def get: F[MonitoredStats] =
    Sync[F].blocking {
      statsStore.get()
    }

  override def update(f: MonitoredStats => MonitoredStats): F[MonitoredStats] =
    Sync[F].blocking {
      statsStore.updateAndGet(stats => f(stats))
    }
}

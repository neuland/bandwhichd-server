package de.neuland.bandwhichd.server.domain.stats

trait StatsRepository[F[_]] {
  def safe(stats: MonitoredStats): F[Unit]

  def get: F[MonitoredStats]
}

package de.neuland.bandwhichd.server.domain.stats

trait StatsRepository[F[_]] {
  def safe(stats: Stats): F[Unit]

  def get: F[Stats]
}

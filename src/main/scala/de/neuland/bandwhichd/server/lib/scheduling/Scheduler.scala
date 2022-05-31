package de.neuland.bandwhichd.server.lib.scheduling

trait Scheduler[F[_]] {
  def schedule: F[Schedule[F]]
}

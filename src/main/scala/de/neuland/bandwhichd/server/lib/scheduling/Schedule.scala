package de.neuland.bandwhichd.server.lib.scheduling

import scala.concurrent.duration.FiniteDuration

sealed trait Schedule[F[_]] {
  def work: Work[F]
}

object Schedule {
  case class Pausing[F[_]](
      name: String,
      pauseDuration: FiniteDuration,
      work: Work[F]
  ) extends Schedule[F]
}

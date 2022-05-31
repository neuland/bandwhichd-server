package de.neuland.bandwhichd.server.lib.scheduling

import cats.effect.{Async, Outcome, Resource}
import cats.implicits.*

import java.util.concurrent.atomic.AtomicBoolean

trait Operator[F[_]] {
  def resource: Resource[F, F[Outcome[F, Throwable, Unit]]]
}

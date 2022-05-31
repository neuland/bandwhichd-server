package de.neuland.bandwhichd.server.domain

opaque type Received[A] = A

object Received {
  def apply[A](value: A): Received[A] = value

  extension [A](received: Received[A]) {
    def value: A = received
  }
}

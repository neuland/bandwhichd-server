package de.neuland.bandwhichd.server.domain

opaque type Sent[A] = A

object Sent {
  def apply[A](value: A): Sent[A] = value

  extension [A](sent: Sent[A]) {
    def value: A = sent
  }
}

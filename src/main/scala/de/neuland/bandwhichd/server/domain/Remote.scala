package de.neuland.bandwhichd.server.domain

opaque type Remote[+A] = A

object Remote {
  def apply[A](value: A): Remote[A] = value

  extension [A](remote: Remote[A]) {
    def value: A = remote
  }
}

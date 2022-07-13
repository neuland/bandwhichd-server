package de.neuland.bandwhichd.server.domain

opaque type Local[+A] = A

object Local {
  def apply[A](value: A): Local[A] = value

  extension [A](local: Local[A]) {
    def value: A = local
  }
}

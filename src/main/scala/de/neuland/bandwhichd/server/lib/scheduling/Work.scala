package de.neuland.bandwhichd.server.lib.scheduling

class Work[F[_]](f: => F[Unit]) {
  def run: F[Unit] = f
}

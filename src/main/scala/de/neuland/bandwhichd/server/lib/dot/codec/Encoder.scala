package de.neuland.bandwhichd.server.lib.dot.codec

import de.neuland.bandwhichd.server.lib.dot.Dot

trait Encoder[A] {
  def apply(a: A): Dot
}

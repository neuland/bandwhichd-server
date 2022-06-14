package de.neuland.bandwhichd.server.lib.health

opaque type CheckKey = String

object CheckKey {
  def apply(value: String): CheckKey = value

  extension (checkKey: CheckKey) {
    def value: String = checkKey
  }
}

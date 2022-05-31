package de.neuland.bandwhichd.server.domain

opaque type InterfaceName = String

object InterfaceName {
  def apply(value: String): InterfaceName = value

  extension (interfaceName: InterfaceName) {
    def value: String = interfaceName
  }
}

package de.neuland.bandwhichd.server.domain

opaque type ProcessName = String

object ProcessName {
  def apply(value: String): ProcessName = value

  extension (processName: ProcessName) {
    def value: String = processName
  }
}

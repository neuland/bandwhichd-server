package de.neuland.bandwhichd.server.domain

import java.util.UUID

opaque type MachineId = UUID

object MachineId {
  def apply(value: UUID): MachineId = value

  extension (machineId: MachineId) {
    def value: UUID = machineId
  }
}

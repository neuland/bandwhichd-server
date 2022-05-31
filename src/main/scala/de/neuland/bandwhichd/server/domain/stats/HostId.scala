package de.neuland.bandwhichd.server.domain.stats

import com.comcast.ip4s.{Host, Hostname, IDN}
import de.neuland.bandwhichd.server.domain.MachineId

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.UUID

sealed trait HostId {
  def uuid: UUID
}

object HostId {
  case class MachineIdHostId(machineId: MachineId) extends HostId {
    override def uuid: UUID =
      machineId.value
  }

  case class HostHostId(host: Host) extends HostId {
    override def uuid: UUID =
      UUID.nameUUIDFromBytes(host.toString.getBytes(UTF_8))
  }

  def apply(machineId: MachineId): HostId =
    MachineIdHostId(machineId)

  def apply(host: Host): HostId =
    HostHostId(host)
}

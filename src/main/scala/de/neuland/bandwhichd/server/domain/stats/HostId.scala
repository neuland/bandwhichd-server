package de.neuland.bandwhichd.server.domain.stats

import com.comcast.ip4s.{Host => Ip4sHost, Hostname, IDN}
import de.neuland.bandwhichd.server.domain.{MachineId => BandwhichdMachineId}

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.UUID

sealed trait HostId {
  def uuid: UUID
}

object HostId {
  case class MachineId(machineId: BandwhichdMachineId) extends HostId {
    override def uuid: UUID =
      machineId.value
  }

  case class Host(host: Ip4sHost) extends HostId {
    override def uuid: UUID =
      UUID.nameUUIDFromBytes(host.toString.getBytes(UTF_8))
  }

  def apply(machineId: BandwhichdMachineId): MachineId =
    MachineId(machineId)

  def apply(host: Ip4sHost): Host =
    Host(host)
}

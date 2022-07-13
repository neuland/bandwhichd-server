package de.neuland.bandwhichd.server.domain.stats

import com.comcast.ip4s.{Host, Hostname}
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.stats.HostId

sealed trait AnyHost[+I <: HostId] extends Equals {
  def hostId: I
  def host: Host

  override def canEqual(that: Any): Boolean =
    that match
      case _: AnyHost[HostId] => true
      case _                  => false

  override def equals(obj: Any): Boolean =
    obj match
      case anyHost: AnyHost[HostId] => hostId == anyHost.hostId
      case _                        => false

  override def hashCode(): Int =
    hostId.hashCode()
}

case class UnidentifiedHost(
    host: Host
) extends AnyHost[HostId] {
  override def hostId: HostId =
    HostId(host)
}

object UnidentifiedHost {
  type HostId = HostId.HostHostId
}

sealed trait IdentifiedHost[+I <: HostId] extends AnyHost[I]

sealed trait MachineIdHost extends IdentifiedHost[HostId.MachineIdHostId] {
  def hostId: MachineIdHost.HostId
}

object MachineIdHost {
  type HostId = HostId.MachineIdHostId
}

case class MonitoredHost(
    hostId: HostId.MachineIdHostId,
    agentIds: Set[AgentId],
    hostname: Hostname,
    additionalHostnames: Set[Hostname],
    interfaces: Set[Interface]
) extends MachineIdHost {
  val hostnames: Set[Hostname] =
    additionalHostnames + hostname

  override def host: Host = hostname
}

object MonitoredHost {
  type HostId = MachineIdHost.HostId
}

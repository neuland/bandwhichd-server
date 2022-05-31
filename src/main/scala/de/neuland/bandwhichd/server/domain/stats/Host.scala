package de.neuland.bandwhichd.server.domain.stats

import com.comcast.ip4s.{Host, Hostname}
import de.neuland.bandwhichd.server.domain.*

sealed trait AnyHost extends Equals {
  def hostId: HostId

  override def canEqual(that: Any): Boolean =
    that match
      case _: AnyHost => true
      case _          => false

  override def equals(obj: Any): Boolean =
    obj match
      case anyHost: AnyHost => hostId == anyHost.hostId
      case _                => false

  override def hashCode(): Int =
    hostId.hashCode()
}

case class MonitoredHost(
    hostId: HostId,
    agentIds: Set[AgentId],
    hostname: Hostname,
    additionalHostnames: Set[Hostname],
    interfaces: Set[Interface]
) extends AnyHost {
  val hostnames: Set[Hostname] =
    additionalHostnames + hostname
}

case class UnidentifiedHost(
    host: Host
) extends AnyHost {
  override def hostId: HostId =
    HostId(host)
}

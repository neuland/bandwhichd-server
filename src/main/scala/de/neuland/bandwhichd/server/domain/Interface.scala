package de.neuland.bandwhichd.server.domain

import com.comcast.ip4s.{Cidr, IpAddress}

case class Interface(
    name: InterfaceName,
    isUp: Boolean,
    networks: Seq[Cidr[IpAddress]]
) {
  override def canEqual(that: Any): Boolean =
    that match
      case _: Interface => true
      case _            => false

  override def equals(obj: Any): Boolean =
    obj match
      case interface: Interface => interface.name == name
      case _                    => false

  override def hashCode(): Int =
    name.hashCode()
}

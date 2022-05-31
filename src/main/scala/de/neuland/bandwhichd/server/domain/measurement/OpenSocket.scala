package de.neuland.bandwhichd.server.domain.measurement

import com.comcast.ip4s.{Host, SocketAddress}
import de.neuland.bandwhichd.server.domain.*

case class OpenSocket(
    socket: SocketAddress[Host],
    protocol: Protocol,
    maybeProcessName: Option[ProcessName]
)

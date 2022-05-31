package de.neuland.bandwhichd.server.domain.measurement

import com.comcast.ip4s.{Host, SocketAddress}
import de.neuland.bandwhichd.server.domain.*

case class Connection(
    interfaceName: InterfaceName,
    localSocket: Local[SocketAddress[Host]],
    remoteSocket: Remote[SocketAddress[Host]],
    protocol: Protocol,
    received: Received[BytesCount],
    sent: Sent[BytesCount]
)

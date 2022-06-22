package de.neuland.bandwhichd.server.lib.test.cassandra

import com.comcast.ip4s.{Host, IpAddress, Port, SocketAddress}
import com.datastax.oss.driver.api.core.metadata.EndPoint
import com.datastax.oss.driver.api.core.{CqlSession, CqlSessionBuilder}
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

import java.net.InetSocketAddress
import java.time.Duration

class CassandraTestContainer(
    dockerImageName: DockerImageName,
    imageCqlPort: Port,
    datacenter: String
) extends GenericContainer[CassandraTestContainer](dockerImageName) {

  addExposedPort(imageCqlPort.value)
  withStartupTimeout(Duration.ofMinutes(2))
  withEnv("CASSANDRA_SNITCH", "GossipingPropertyFileSnitch")
  withEnv(
    "JVM_OPTS",
    "-Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0"
  )
  withEnv("HEAP_NEWSIZE", "128M")
  withEnv("MAX_HEAP_SIZE", "1024M")

  def host: IpAddress = {
    val containerHost = getHost
    if (containerHost == "localhost") {
      import com.comcast.ip4s.ipv4
      ipv4"127.0.0.1"
    } else {
      IpAddress.fromString(containerHost).get
    }
  }

  def port: Port =
    Port.fromInt(getMappedPort(imageCqlPort.value)).get

  def socket: SocketAddress[IpAddress] =
    SocketAddress(host, port)

  def endpoint: EndPoint =
    DefaultEndPoint(socket.toInetSocketAddress)

  def cqlSessionBuilder: CqlSessionBuilder =
    CqlSession
      .builder()
      .addContactEndPoint(endpoint)
      .withLocalDatacenter(datacenter)
}

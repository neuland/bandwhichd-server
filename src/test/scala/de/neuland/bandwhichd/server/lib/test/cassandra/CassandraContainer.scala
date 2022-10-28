package de.neuland.bandwhichd.server.lib.test.cassandra

import com.comcast.ip4s.Port
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.dimafeng.testcontainers.{GenericContainer, SingleContainer}
import org.testcontainers.utility.DockerImageName

case class CassandraContainer(
    dockerImageName: DockerImageName,
    imageCqlPort: Port,
    datacenter: String
) extends SingleContainer[CassandraTestContainer] {
  override val container: CassandraTestContainer =
    CassandraTestContainer(dockerImageName, imageCqlPort, datacenter)
}

object CassandraContainer {
  val defaultImageName: DockerImageName =
    DockerImageName.parse("cassandra:4.1")

  val defaultImageCqlPort: Port =
    Port.fromInt(9042).get

  val defaultImageDatacenter: String =
    "datacenter1"

  def apply(): CassandraContainer =
    CassandraContainer(
      defaultImageName,
      defaultImageCqlPort,
      defaultImageDatacenter
    )
}

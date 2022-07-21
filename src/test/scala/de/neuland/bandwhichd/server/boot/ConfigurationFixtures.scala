package de.neuland.bandwhichd.server.boot

import com.datastax.oss.driver.api.core.CqlIdentifier
import de.neuland.bandwhichd.server.lib.test.cassandra.CassandraContainer

import java.time.Duration

object ConfigurationFixtures {
  val testDefaults: Configuration =
    Configuration(
      readonly = false,
      contactPoints = Seq.empty,
      localDatacenter = "",
      migrationQueryTimeout = Duration.ofSeconds(10),
      measurementsKeyspace = CqlIdentifier.fromCql("bandwhichd"),
      measurementNetworkConfigurationTTL = Duration.ofHours(2),
      measurementNetworkUtilizationTTL = Duration.ofHours(2),
      recordMeasurementQueryTimeout = Duration.ofSeconds(4),
      getAllMeasurementsQueryTimeout = Duration.ofSeconds(8)
    )

  def testDefaults(container: CassandraContainer): Configuration =
    testDefaults.copy(
      contactPoints = Seq(container.container.socket),
      localDatacenter = container.datacenter
    )
}

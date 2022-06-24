package de.neuland.bandwhichd.server.boot

import com.datastax.oss.driver.api.core.CqlIdentifier
import de.neuland.bandwhichd.server.lib.test.cassandra.CassandraContainer

import java.time.Duration

object ConfigurationFixtures {
  def testDefaults(container: CassandraContainer): Configuration =
    Configuration(
      readonly = false,
      contactPoints = Seq(container.container.socket),
      localDatacenter = container.datacenter,
      measurementsKeyspace = CqlIdentifier.fromCql("bandwhichd"),
      measurementNetworkConfigurationTTL = Duration.ofHours(2),
      measurementNetworkUtilizationTTL = Duration.ofHours(2),
      recordMeasurementQueryTimeout = Duration.ofSeconds(4),
      getAllMeasurementsQueryTimeout = Duration.ofSeconds(8),
      aggregationSchedulerInterval = Duration.ofSeconds(10)
    )
}

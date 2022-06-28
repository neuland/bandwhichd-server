package de.neuland.bandwhichd.server.adapter.out

import cats.implicits.*
import cats.effect.Async
import com.datastax.oss.driver.api.core.cql.{SimpleStatement, Statement}
import de.neuland.bandwhichd.server.boot.Configuration
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext

class CassandraMigration[F[_]: Async](
    private val cassandraContext: CassandraContext[F]
) {
  def migrate(configuration: Configuration): F[Unit] =
    for {
      _ <- createCidrType(configuration)
      _ <- createMeasurementNetworkConfigurationInterfaceType(configuration)
      _ <- createMeasurementNetworkConfigurationOpenSocketType(configuration)
      _ <- createMeasurementNetworkUtilizationConnectionType(configuration)
      _ <- createMeasurementsTable(configuration)
    } yield ()

  private def createMeasurementsTable(
      configuration: Configuration
  ): F[Unit] =
    cassandraContext.executeRawExpectNoRow(
      SimpleStatement
        .builder(
          """create table if not exists measurements_by_date (
          |  date date,
          |  timestamp timestamp,
          |  end_timestamp timestamp,
          |  agent_id uuid,
          |  measurement_type ascii,
          |  network_configuration_machine_id uuid,
          |  network_configuration_hostname text,
          |  network_configuration_interfaces frozen<list<frozen<measurement_network_configuration_interface>>>,
          |  network_configuration_open_sockets frozen<list<frozen<measurement_network_configuration_open_socket>>>,
          |  network_utilization_connections frozen<list<frozen<measurement_network_utilization_connection>>>,
          |  primary key ((date), timestamp, agent_id, measurement_type),
          |) with clustering order by (timestamp asc)""".stripMargin
        )
        .setKeyspace(configuration.measurementsKeyspace)
        .setTimeout(configuration.migrationQueryTimeout)
        .build()
    )

  private def createMeasurementNetworkConfigurationInterfaceType(
      configuration: Configuration
  ): F[Unit] =
    cassandraContext.executeRawExpectNoRow(
      SimpleStatement
        .builder(
          """create type if not exists measurement_network_configuration_interface (
            |  name text,
            |  is_up boolean,
            |  networks frozen<list<frozen<cidr>>>,
            |)""".stripMargin
        )
        .setKeyspace(configuration.measurementsKeyspace)
        .setTimeout(configuration.migrationQueryTimeout)
        .build()
    )

  private def createMeasurementNetworkConfigurationOpenSocketType(
      configuration: Configuration
  ): F[Unit] =
    cassandraContext.executeRawExpectNoRow(
      SimpleStatement
        .builder(
          """create type if not exists measurement_network_configuration_open_socket (
            |  socket text,
            |  protocol ascii,
            |  maybe_process_name text,
            |)""".stripMargin
        )
        .setKeyspace(configuration.measurementsKeyspace)
        .setTimeout(configuration.migrationQueryTimeout)
        .build()
    )

  private def createMeasurementNetworkUtilizationConnectionType(
      configuration: Configuration
  ): F[Unit] =
    cassandraContext.executeRawExpectNoRow(
      SimpleStatement
        .builder(
          """create type if not exists measurement_network_utilization_connection (
            |  interface_name text,
            |  local_socket text,
            |  remote_socket text,
            |  protocol ascii,
            |  received bigint,
            |  sent bigint,
            |)""".stripMargin
        )
        .setKeyspace(configuration.measurementsKeyspace)
        .setTimeout(configuration.migrationQueryTimeout)
        .build()
    )

  private def createCidrType(
      configuration: Configuration
  ): F[Unit] =
    cassandraContext.executeRawExpectNoRow(
      SimpleStatement
        .builder(
          """create type if not exists cidr (
            |  address inet,
            |  prefix_bits smallint,
            |)""".stripMargin
        )
        .setKeyspace(configuration.measurementsKeyspace)
        .setTimeout(configuration.migrationQueryTimeout)
        .build()
    )
}

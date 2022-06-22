package de.neuland.bandwhichd.server.test

import cats.implicits.*
import cats.effect.Async
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.cql.{SimpleStatement, Statement}
import de.neuland.bandwhichd.server.adapter.out.CassandraMigration
import de.neuland.bandwhichd.server.boot.Configuration
import de.neuland.bandwhichd.server.lib.cassandra.CassandraContext

class CassandraTestMigration[F[_]: Async](
    private val cassandraContext: CassandraContext[F]
) {
  private val cassandraMigration: CassandraMigration[F] =
    CassandraMigration(cassandraContext)

  def migrate(
      configuration: Configuration
  ): F[Unit] =
    for {
      _ <- createTestKeyspaceIfNotExists(configuration.measurementsKeyspace)
      _ <- cassandraMigration.migrate(configuration)
    } yield ()

  private def createTestKeyspaceIfNotExists(
      keyspace: CqlIdentifier
  ): F[Unit] =
    cassandraContext.executeRawExpectNoRow(
      SimpleStatement
        .builder(
          s"""create keyspace if not exists ${keyspace.asCql(false)}
           |with replication = {'class': 'SimpleStrategy', 'replication_factor': 1}""".stripMargin
        )
        .build()
    )
}

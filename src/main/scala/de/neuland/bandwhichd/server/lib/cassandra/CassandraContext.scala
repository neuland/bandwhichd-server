package de.neuland.bandwhichd.server.lib.cassandra

import cats.ApplicativeError
import cats.effect.{Async, Resource}
import cats.implicits.*
import com.comcast.ip4s.{IpAddress, SocketAddress}
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveRow
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, Row, Statement}
import com.datastax.oss.driver.api.core.{CqlSession, CqlSessionBuilder}
import com.datastax.oss.driver.internal.core.util.concurrent.CompletableFutures
import de.neuland.bandwhichd.server.boot.Configuration
import fs2.Stream
import fs2.interop.reactivestreams.*

import java.util.concurrent.{CompletableFuture, CompletionStage}
import scala.annotation.targetName
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class CassandraContext[F[_]: Async](
    private val cqlSession: CqlSession
) extends AutoCloseable {

  def executeRaw(statement: Statement[_]): Stream[F, ReactiveRow] =
    executeRaw(statement, 1)

  def executeRaw(
      statement: Statement[_],
      bufferSize: Int
  ): Stream[F, ReactiveRow] =
    cqlSession
      .executeReactive(statement)
      .toStreamBuffered(bufferSize)

  def executeRawExpectSingleRow(
      statement: Statement[_]
  ): F[Row] =
    Async[F]
      .fromCompletableFuture(
        cqlSession
          .executeAsync(statement)
          .toCompletableFuture
          .pure
      )
      .flatMap(CassandraContext.singleRow)

  def executeRawExpectNoRow(
      statement: Statement[_]
  ): F[Unit] =
    Async[F]
      .fromCompletableFuture(
        cqlSession
          .executeAsync(statement)
          .toCompletableFuture
          .pure
      )
      .flatMap(CassandraContext.noRow)

  override def close(): Unit =
    cqlSession.close()
}

object CassandraContext {
  def resource[F[_]: Async](
      configuration: Configuration
  ): Resource[F, CassandraContext[F]] =
    resource(
      contactPoints = configuration.contactPoints,
      localDatacenter = configuration.localDatacenter
    )

  def resource[F[_]: Async](
      contactPoints: Seq[SocketAddress[IpAddress]],
      localDatacenter: String
  ): Resource[F, CassandraContext[F]] =
    resource(
      CqlSession
        .builder()
        .addContactPoints(
          contactPoints.map(_.toInetSocketAddress).asJavaCollection
        )
        .withLocalDatacenter(localDatacenter)
    )

  def resource[F[_]: Async](
      cqlSessionBuilder: CqlSessionBuilder
  ): Resource[F, CassandraContext[F]] =
    Resource.fromAutoCloseable(
      Async[F].defer {
        CassandraContext(cqlSessionBuilder.build()).pure
      }
    )

  def singleRow[AE[_]](
      asyncResultSet: AsyncResultSet
  )(implicit
      ae: ApplicativeError[AE, Throwable]
  ): AE[Row] =
    if (asyncResultSet.hasMorePages)
      ApplicativeError[AE, Throwable].raiseError(
        new Exception("More than one page")
      )
    else {
      val rowIterator = asyncResultSet.currentPage().iterator()
      if (!rowIterator.hasNext)
        ApplicativeError[AE, Throwable].raiseError(new Exception("No row"))
      else {
        val row = rowIterator.next()
        if (rowIterator.hasNext)
          ApplicativeError[AE, Throwable].raiseError(
            new Exception("More than one row")
          )
        else
          row.pure
      }
    }

  def noRow[AE[_]](
      asyncResultSet: AsyncResultSet
  )(implicit
      ae: ApplicativeError[AE, Throwable]
  ): AE[Unit] =
    if (asyncResultSet.hasMorePages)
      ApplicativeError[AE, Throwable].raiseError(
        new Exception("More than one page")
      )
    else {
      val rowIterator = asyncResultSet.currentPage().iterator()
      if (rowIterator.hasNext)
        ApplicativeError[AE, Throwable].raiseError(
          new Exception("One or more rows")
        )
      else
        ae.unit
    }
}

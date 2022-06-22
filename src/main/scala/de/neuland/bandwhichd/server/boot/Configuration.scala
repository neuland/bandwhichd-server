package de.neuland.bandwhichd.server.boot

import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import cats.{Defer, Monad, MonadError, Traverse}
import com.comcast.ip4s.{Dns, Host, IpAddress, SocketAddress}
import com.datastax.oss.driver.api.core.CqlIdentifier

import scala.util.{Failure, Success, Try}

case class Configuration(
    contactPoints: Seq[SocketAddress[IpAddress]],
    localDatacenter: String,
    measurementsKeyspace: CqlIdentifier
)

object Configuration {
  def resolve[F[_]: Sync](
      contactPoints: String,
      localDatacenter: String,
      measurementsKeyspace: String
  ): F[Configuration] = {

    val maybeHostnameContactPoints = contactPoints
      .split(',')
      .toSeq
      .map(contactPointValue =>
        SocketAddress
          .fromString(contactPointValue)
          .fold[Try[SocketAddress[Host]]](
            Failure(
              new Exception(
                s"invalid contact point socket address $contactPointValue"
              )
            )
          )(Success.apply)
      )

    for {
      hostnameContactPoints <- Traverse[Seq].traverse(
        maybeHostnameContactPoints
      )(
        _.fold(Sync[F].raiseError, Sync[F].pure)
      )
      ipAddressContactPoints <- Traverse[Seq].traverse(hostnameContactPoints)(
        _.resolve
      )
    } yield Configuration(
      contactPoints = ipAddressContactPoints,
      localDatacenter = localDatacenter,
      measurementsKeyspace = CqlIdentifier.fromCql(measurementsKeyspace)
    )
  }

  def resolveEnv[F[_]: Sync]: F[Configuration] =
    resolve(
      scala.util.Properties.envOrElse("CONTACT_POINTS", "localhost:9042"),
      scala.util.Properties.envOrElse("LOCAL_DATACENTER", "datacenter1"),
      scala.util.Properties.envOrElse("MEASUREMENTS_KEYSPACE", "bandwhichd")
    )

  def resource[F[_]: Sync]: Resource[F, Configuration] =
    Resource.eval(Sync[F].defer {
      resolveEnv
    })
}

package de.neuland.bandwhichd.server.boot

import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import cats.{Defer, Monad, MonadError, Traverse}
import com.comcast.ip4s.{Dns, Host, IpAddress, SocketAddress}
import com.datastax.oss.driver.api.core.CqlIdentifier

import java.time.Duration
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

case class Configuration(
    readonly: Boolean,
    contactPoints: Seq[SocketAddress[IpAddress]],
    localDatacenter: String,
    migrationQueryTimeout: Duration,
    measurementsKeyspace: CqlIdentifier,
    measurementNetworkConfigurationTTL: Duration,
    measurementNetworkUtilizationTTL: Duration,
    recordMeasurementQueryTimeout: Duration,
    getAllMeasurementsQueryTimeout: Duration,
    maximumTimespanBetweenNetworkConfigurationUpdates: Duration
)

object Configuration {
  def resolve[F[_]: Sync](
      readonly: String,
      contactPoints: String,
      localDatacenter: String,
      migrationQueryTimeout: String,
      measurementsKeyspace: String,
      measurementNetworkConfigurationTTL: String,
      measurementNetworkUtilizationTTL: String,
      recordMeasurementQueryTimeout: String,
      getAllMeasurementsQueryTimeout: String,
      maximumTimespanBetweenNetworkConfigurationUpdates: String
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
      readonly = readonly == "true",
      contactPoints = ipAddressContactPoints,
      localDatacenter = localDatacenter,
      migrationQueryTimeout = Duration.parse(migrationQueryTimeout),
      measurementsKeyspace = CqlIdentifier.fromCql(measurementsKeyspace),
      measurementNetworkConfigurationTTL =
        Duration.parse(measurementNetworkConfigurationTTL),
      measurementNetworkUtilizationTTL =
        Duration.parse(measurementNetworkUtilizationTTL),
      recordMeasurementQueryTimeout =
        Duration.parse(recordMeasurementQueryTimeout),
      getAllMeasurementsQueryTimeout =
        Duration.parse(getAllMeasurementsQueryTimeout),
      maximumTimespanBetweenNetworkConfigurationUpdates =
        Duration.parse(maximumTimespanBetweenNetworkConfigurationUpdates)
    )
  }

  def resolveEnv[F[_]: Sync]: F[Configuration] =
    resolve(
      scala.util.Properties.envOrElse("READONLY", ""),
      scala.util.Properties.envOrElse("CONTACT_POINTS", "localhost:9042"),
      scala.util.Properties.envOrElse("LOCAL_DATACENTER", "datacenter1"),
      scala.util.Properties.envOrElse("MIGRATION_QUERY_TIMEOUT", "PT10S"),
      scala.util.Properties.envOrElse("MEASUREMENTS_KEYSPACE", "bandwhichd"),
      scala.util.Properties
        .envOrElse("MEASUREMENT_NETWORK_CONFIGURATION_TTL", "PT2H"),
      scala.util.Properties
        .envOrElse("MEASUREMENT_NETWORK_UTILIZATION_TTL", "PT2H"),
      scala.util.Properties
        .envOrElse("RECORD_MEASUREMENT_QUERY_TIMEOUT", "PT2S"),
      scala.util.Properties
        .envOrElse("GET_ALL_MEASUREMENTS_QUERY_TIMEOUT", "PT8S"),
      scala.util.Properties
        .envOrElse(
          "MAXIMUM_TIMESPAN_BETWEEN_NETWORK_CONFIGURATION_UPDATES",
          "PT15M"
        )
    )

  def resource[F[_]: Sync]: Resource[F, Configuration] =
    Resource.eval(Sync[F].defer {
      resolveEnv
    })
}

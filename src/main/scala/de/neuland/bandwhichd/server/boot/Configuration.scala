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
    contactPoints: Seq[SocketAddress[IpAddress]],
    localDatacenter: String,
    measurementsKeyspace: CqlIdentifier,
    measurementNetworkConfigurationTTL: Duration,
    measurementNetworkUtilizationTTL: Duration,
    recordMeasurementQueryTimeout: Duration,
    getAllMeasurementsQueryTimeout: Duration,
    aggregationSchedulerInterval: Duration
)

object Configuration {
  def resolve[F[_]: Sync](
      contactPoints: String,
      localDatacenter: String,
      measurementsKeyspace: String,
      measurementNetworkConfigurationTTL: String,
      measurementNetworkUtilizationTTL: String,
      recordMeasurementQueryTimeout: String,
      getAllMeasurementsQueryTimeout: String,
      aggregationSchedulerInterval: String
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
      measurementsKeyspace = CqlIdentifier.fromCql(measurementsKeyspace),
      measurementNetworkConfigurationTTL =
        Duration.parse(measurementNetworkConfigurationTTL),
      measurementNetworkUtilizationTTL =
        Duration.parse(measurementNetworkUtilizationTTL),
      recordMeasurementQueryTimeout =
        Duration.parse(recordMeasurementQueryTimeout),
      getAllMeasurementsQueryTimeout =
        Duration.parse(getAllMeasurementsQueryTimeout),
      aggregationSchedulerInterval =
        Duration.parse(aggregationSchedulerInterval)
    )
  }

  def resolveEnv[F[_]: Sync]: F[Configuration] =
    resolve(
      scala.util.Properties.envOrElse("CONTACT_POINTS", "localhost:9042"),
      scala.util.Properties.envOrElse("LOCAL_DATACENTER", "datacenter1"),
      scala.util.Properties.envOrElse("MEASUREMENTS_KEYSPACE", "bandwhichd"),
      scala.util.Properties
        .envOrElse("MEASUREMENT_NETWORK_CONFIGURATION_TTL", "PT2H"),
      scala.util.Properties
        .envOrElse("MEASUREMENT_NETWORK_UTILIZATION_TTL", "PT2H"),
      scala.util.Properties
        .envOrElse("RECORD_MEASUREMENT_QUERY_TIMEOUT", "PT2S"),
      scala.util.Properties
        .envOrElse("GET_ALL_MEASUREMENTS_QUERY_TIMEOUT", "PT8S"),
      scala.util.Properties.envOrElse("AGGREGATION_SCHEDULER_INTERVAL", "PT10S")
    )

  def resource[F[_]: Sync]: Resource[F, Configuration] =
    Resource.eval(Sync[F].defer {
      resolveEnv
    })
}

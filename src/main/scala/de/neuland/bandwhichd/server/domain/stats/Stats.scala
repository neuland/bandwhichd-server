package de.neuland.bandwhichd.server.domain.stats

import cats.effect.Concurrent
import com.comcast.ip4s.*
import de.neuland.bandwhichd.server.domain.*
import de.neuland.bandwhichd.server.domain.measurement.{Measurement, Timing}
import de.neuland.bandwhichd.server.domain.stats.Stats.Bundle
import de.neuland.bandwhichd.server.lib.time.Interval
import de.neuland.bandwhichd.server.lib.time.cats.TimeContext
import fs2.Stream

import java.nio.charset.StandardCharsets.UTF_8
import java.time.temporal.ChronoUnit.HOURS
import java.time.{Duration, Instant}
import java.util.UUID
import scala.reflect.ClassTag

class Stats[L <: HostId, H <: AnyHost[L], R <: HostId] private (
    private val bundles: Map[L, Stats.Bundle[L, H, R]]
) {
  def hosts: Set[H] =
    bundles.values.map(_.host).toSet

  def hostIds: Set[L] =
    bundles.values.map(_.host.hostId).toSet

  def connections: Set[(L, R)] =
    bundles.values.flatMap { bundle =>
      bundle.connections.keys.map { remoteHostId =>
        bundle.host.hostId -> remoteHostId
      }
    }.toSet

  def connectionsFor(hostId: L): Option[Map[R, Stats.Connection]] =
    bundles
      .get(hostId)
      .map(bundle =>
        bundle.connections.view.mapValues(_ => Stats.Connection()).toMap
      )

  def dropBefore(timestamp: Timing.Timestamp): Stats[L, H, R] =
    new Stats(
      bundles
        .filterNot { case (_, bundle) =>
          bundle.lastSeenAt.instant.isBefore(timestamp.instant)
        }
        .view
        .mapValues(bundle =>
          bundle.copy(
            connections = bundle.connections.filterNot { case (_, connection) =>
              connection.lastSeenAt.instant.isBefore(timestamp.instant)
            }
          )
        )
        .toMap
    )
}

type MonitoredStats = Stats[HostId.MachineId, MonitoredHost, HostId]

object Stats {
  val defaultTimeframeDuration: Duration = Duration.ofHours(2)

  val empty: MonitoredStats = new Stats(Map.empty)

  extension (stats: MonitoredStats) {
    def append(
        measurement: Measurement[Timing]
    ): MonitoredStats =
      measurement match
        case Measurement.NetworkConfiguration(
              machineId,
              timing,
              hostname,
              interfaces,
              _
            ) =>
          val hostId: HostId.MachineId = HostId(machineId)

          val maybeBundle
              : Option[Bundle[HostId.MachineId, MonitoredHost, HostId]] =
            stats.bundles
              .get(hostId)
              .orElse {
                stats.bundles.values.find(_.host.hostnames.contains(hostname))
              }

          val bundle = maybeBundle.fold {
            Stats.Bundle(
              host = MonitoredHost(
                hostId = hostId,
                hostname = hostname,
                additionalHostnames = Set.empty,
                interfaces = interfaces.toSet
              ),
              lastSeenAt = timing,
              connections = Map.empty
            )
          } { bundle =>
            bundle.copy(
              host = MonitoredHost(
                hostId = hostId,
                hostname = hostname,
                additionalHostnames = bundle.host.hostnames - hostname,
                interfaces = bundle.host.interfaces ++ interfaces
              ),
              lastSeenAt = timing
            )
          }

          new Stats(stats.bundles + (hostId -> bundle))

        case Measurement.NetworkUtilization(
              machineId,
              timing,
              connections
            ) =>
          val hostId: HostId.MachineId = HostId(machineId)

          stats.bundles
            .get(hostId)
            .fold(stats) { bundle =>
              new Stats(
                stats.bundles + (bundle.host.hostId -> bundle.copy(
                  lastSeenAt = timing.end,
                  connections = bundle.connections ++ connections.map {
                    connection =>

                      val remoteHost: Host = connection.remoteSocket.value.host
                      val maybeRemoteIpAddress = remoteHost match
                        case ipAddress: IpAddress => Some(ipAddress)
                        case _                    => None

                      val maybeRemoteBundle =
                        maybeRemoteIpAddress.flatMap { remoteIpAddress =>
                          stats.bundles.values
                            .find(
                              _.host.interfaces.exists(
                                _.networks
                                  .exists(
                                    _.address == remoteIpAddress
                                  )
                              )
                            )

                        }

                      val remoteHostId = maybeRemoteBundle
                        .fold(HostId(connection.remoteSocket.value.host))(
                          _.host.hostId
                        )

                      remoteHostId -> Bundle.Connection(
                        lastSeenAt = timing.end
                      )
                  }
                ))
              )
            }

    def monitoredNetworks: Set[Cidr[IpAddress]] =
      stats.hosts
        .flatMap(_.interfaces)
        .flatMap(_.networks)

    def withoutHostsOutsideOfMonitoredNetworks: MonitoredStats = {
      val monitoredNetworks = stats.monitoredNetworks
      new Stats(
        stats.bundles.view.mapValues { bundle =>
          bundle.copy(
            connections = bundle.connections.filter {
              _._1 match
                case _: HostId.MachineId => true
                case HostId.Host(ipAddress: IpAddress) =>
                  monitoredNetworks.exists(_.contains(ipAddress))
                case _ => false
            }
          )
        }.toMap
      )
    }

    def unidentifiedRemoteHosts: Set[UnidentifiedHost] =
      stats.bundles.values.flatMap { bundle =>
        bundle.connections.flatMap {
          _._1 match
            case HostId.Host(host)   => Some(UnidentifiedHost(host))
            case HostId.MachineId(_) => None
        }
      }.toSet

    def allHosts: Set[AnyHost[HostId]] =
      stats.hosts ++ stats.unidentifiedRemoteHosts
  }

  case class Connection()

  private[Stats] case class Bundle[L <: HostId, H <: AnyHost[L], R <: HostId](
      host: H,
      lastSeenAt: Timing.Timestamp,
      connections: Map[R, Bundle.Connection]
  )

  private[Stats] object Bundle {
    private[Stats] case class Connection(
        lastSeenAt: Timing.Timestamp
    )
  }
}

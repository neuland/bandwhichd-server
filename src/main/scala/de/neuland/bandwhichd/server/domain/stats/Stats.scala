package de.neuland.bandwhichd.server.domain.stats

import cats.Monad
import cats.effect.kernel.Concurrent
import cats.implicits.*
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
      bundle.remoteHostIds.map { remoteHostId =>
        bundle.host.hostId -> remoteHostId
      }
    }.toSet
}

type AnyStats = Stats[HostId, AnyHost[HostId], HostId]
type MonitoredStats = Stats[HostId.MachineIdHostId, MonitoredHost, HostId]

object Stats {
  val defaultTimeframeDuration: Duration = Duration.ofHours(2)

  def defaultTimeframe[F[_]: Monad](
      timeContext: TimeContext[F]
  ): F[Timing.Timeframe] =
    for {
      now <- timeContext.now
    } yield Timing.Timeframe(Interval(now.minus(defaultTimeframeDuration), now))

  val empty: MonitoredStats = new Stats(Map.empty)

  def apply[F[_]: Concurrent](
      measurements: Stream[F, Measurement[Timing]]
  ): F[MonitoredStats] =
    measurements.compile.fold(Stats.empty) { case (stats, measurement) =>
      stats.append(measurement)
    }

  extension (stats: MonitoredStats) {
    def append(
        measurement: Measurement[Timing]
    ): MonitoredStats =
      measurement match
        case Measurement.NetworkConfiguration(
              agentId,
              timing,
              machineId,
              hostname,
              interfaces,
              _
            ) =>
          val hostId: HostId.MachineIdHostId = HostId(machineId)

          val maybeBundle
              : Option[Bundle[HostId.MachineIdHostId, MonitoredHost, HostId]] =
            stats.bundles
              .get(hostId)
              .orElse {
                stats.bundles.values.find(_.host.agentIds.contains(agentId))
              }
              .orElse {
                stats.bundles.values.find(_.host.hostnames.contains(hostname))
              }

          val bundle = maybeBundle.fold {
            // TODO: Remotes of other hosts?
            Stats.Bundle(
              host = MonitoredHost(
                hostId = hostId,
                agentIds = Set(agentId),
                hostname = hostname,
                additionalHostnames = Set.empty,
                interfaces = interfaces.toSet
              ),
              lastSeenAt = timing,
              remoteHostIds = Set.empty
            )
          } { bundle =>
            bundle.copy(
              host = MonitoredHost(
                hostId = hostId,
                agentIds = bundle.host.agentIds + agentId,
                hostname = hostname,
                additionalHostnames = bundle.host.hostnames - hostname,
                interfaces = bundle.host.interfaces ++ interfaces
              ),
              lastSeenAt = timing
            )
          }

          new Stats(stats.bundles + (hostId -> bundle))

        case Measurement.NetworkUtilization(
              agentId,
              timing,
              connections
            ) =>
          stats.bundles.values
            .find(_.host.agentIds.contains(agentId))
            .fold(stats) { bundle =>
              new Stats(
                stats.bundles + (bundle.host.hostId -> bundle.copy(
                  lastSeenAt = timing.end,
                  remoteHostIds = bundle.remoteHostIds ++ connections.map {
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

                      remoteHostId
                  }
                ))
              )
            }

    def monitoredNetworks: Set[Cidr[IpAddress]] =
      stats.hosts
        .flatMap(_.interfaces)
        .flatMap(_.networks)

    def withoutHostsOutsideOfMonitoredNetworks: MonitoredStats =
      new Stats(
        stats.bundles.view.mapValues { bundle =>
          bundle.copy(
            remoteHostIds = bundle.remoteHostIds.filter {
              _ match
                case _: HostId.MachineIdHostId => true
                case HostId.HostHostId(ipAddress: IpAddress) =>
                  stats.monitoredNetworks.exists(_.contains(ipAddress))
                case _ => false
            }
          )
        }.toMap
      )

    def unidentifiedRemoteHosts: Set[UnidentifiedHost] =
      stats.bundles.values.flatMap { bundle =>
        bundle.remoteHostIds.flatMap {
          _ match
            case HostId.HostHostId(host)   => Some(UnidentifiedHost(host))
            case HostId.MachineIdHostId(_) => None
        }
      }.toSet

    def allHosts: Set[AnyHost[HostId]] =
      stats.hosts ++ stats.unidentifiedRemoteHosts
  }

  private case class Bundle[L <: HostId, H <: AnyHost[L], R <: HostId](
      host: H,
      lastSeenAt: Timing.Timestamp,
      remoteHostIds: Set[R]
  )
}

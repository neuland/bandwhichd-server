package de.neuland.bandwhichd.server.domain.stats

import cats.Monad
import cats.effect.kernel.Concurrent
import cats.implicits.*
import com.comcast.ip4s.{Cidr, Host, Hostname, IDN, IpAddress}
import de.neuland.bandwhichd.server.domain.measurement.{Measurement, Timing}
import de.neuland.bandwhichd.server.domain.{AgentId, Interface, InterfaceName}
import fs2.Stream

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

case class Stats(
    hosts: Set[MonitoredHost],
    connections: Set[(MonitoredHost, AnyHost)]
) {
  def findConnection(
      hostIds: (HostId, HostId)
  ): Option[(MonitoredHost, AnyHost)] =
    connections.find(connection =>
      (connection._1.hostId, connection._2.hostId) == hostIds
    )

  def monitoredNetworks: Set[Cidr[IpAddress]] =
    hosts
      .flatMap(_.interfaces)
      .flatMap(_.networks)

  def withoutHostsOutsideOfMonitoredNetworks: Stats =
    copy(connections = connections.filter {
      _ match {
        case (_, _: MonitoredHost) => true
        case (_, UnidentifiedHost(ipAddress: IpAddress)) =>
          monitoredNetworks.exists(_.contains(ipAddress))
        case _ => false
      }
    })
}

object Stats {
  def apply[F[_]: Concurrent](
      measurements: Stream[F, Measurement[Timing]]
  ): F[Stats] =
    for {
      measurements <- {
        measurements.compile.fold[
          (
              Seq[Measurement.NetworkConfiguration],
              Seq[Measurement.NetworkUtilization]
          )
        ](Seq.empty -> Seq.empty) { case ((ncs, nus), m) =>
          m match
            case nc: Measurement.NetworkConfiguration =>
              ncs.appended(nc) -> nus
            case nu: Measurement.NetworkUtilization =>
              ncs -> nus.appended(nu)
        }
      }
    } yield {
      val ncs: Seq[Measurement.NetworkConfiguration] = measurements._1
      val nus: Seq[Measurement.NetworkUtilization] = measurements._2

      val hosts =
        ncs.foldLeft[Set[MonitoredHost]](
          Set.empty
        ) { case (alreadyIdentifiedMonitoredHosts, networkConfiguration) =>
          val matches = alreadyIdentifiedMonitoredHosts.filter(monitoredHost =>
            monitoredHost.hostId == HostId(networkConfiguration.machineId)
          )

          (alreadyIdentifiedMonitoredHosts -- matches) + MonitoredHost(
            hostId = HostId(networkConfiguration.machineId),
            agentIds =
              matches.flatMap(_.agentIds) + networkConfiguration.agentId,
            hostname = networkConfiguration.hostname,
            additionalHostnames = (matches.map(_.hostname) ++
              matches.flatMap(_.additionalHostnames)) -
              networkConfiguration.hostname,
            interfaces =
              matches.flatMap(_.interfaces) ++ networkConfiguration.interfaces
          )
        }

      def findMonitoredHostByAgentId(agentId: AgentId): Option[MonitoredHost] =
        hosts.find(_.agentIds.contains(agentId))

      def findMonitoredHostByHost(host: Host): Option[MonitoredHost] =
        hosts.find(monitoredHost =>
          host match
            case hostname: Hostname =>
              monitoredHost.hostnames.contains(hostname)
            case ipAddress: IpAddress =>
              monitoredHost.interfaces.toSeq
                .flatMap(_.networks)
                .exists(cidr => cidr.address == ipAddress)
            case _ => false
        )

      def host2AnyHost(host: Host): AnyHost =
        findMonitoredHostByHost(host).getOrElse(UnidentifiedHost(host))

      val connections =
        nus.foldLeft[Set[(MonitoredHost, AnyHost)]](
          Set.empty[(MonitoredHost, AnyHost)]
        ) { case (alreadyIdentifiedConnections, networkUtilization) =>
          val maybeMonitoredHostByAgentId =
            findMonitoredHostByAgentId(networkUtilization.agentId)

          maybeMonitoredHostByAgentId.fold {
            alreadyIdentifiedConnections
          } { monitoredHostByAgentId =>
            networkUtilization.connections
              .foldLeft[Set[(MonitoredHost, AnyHost)]](
                alreadyIdentifiedConnections
              ) { case (alreadyIdentifiedConnections2, connection) =>
                alreadyIdentifiedConnections2 + (monitoredHostByAgentId -> host2AnyHost(
                  connection.remoteSocket.value.host
                ))
              }
          }

        }

      Stats(
        hosts = hosts,
        connections = connections
      )
    }

  val empty: Stats =
    Stats(
      hosts = Set.empty,
      connections = Set.empty
    )
}

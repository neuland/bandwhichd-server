package de.neuland.bandwhichd.server.domain.stats

import cats.Monad
import com.comcast.ip4s.{Host, Hostname, IDN, IpAddress}
import de.neuland.bandwhichd.server.domain.measurement.{Measurement, Timing}
import de.neuland.bandwhichd.server.domain.{AgentId, InterfaceName}

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
}

object Stats {
  def apply[F[_]: Monad](measurements: Seq[Measurement[Timing]]): F[Stats] =
    Monad[F].pure {

      val networkConfigurationMeasurements
          : Seq[Measurement.NetworkConfiguration] =
        measurements.flatMap(measurement =>
          measurement match
            case networkConfiguration @ Measurement
                  .NetworkConfiguration(_, _, _, _, _, _) =>
              Seq(networkConfiguration)
            case _ => Seq.empty
        )

      val networkUtilizationMeasurements: Seq[Measurement.NetworkUtilization] =
        measurements.flatMap(measurement =>
          measurement match
            case networkUtilization @ Measurement.NetworkUtilization(_, _, _) =>
              Seq(networkUtilization)
            case _ => Seq.empty
        )

      val hosts =
        networkConfigurationMeasurements.foldLeft[Set[MonitoredHost]](
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
        networkUtilizationMeasurements.foldLeft[Set[(MonitoredHost, AnyHost)]](
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

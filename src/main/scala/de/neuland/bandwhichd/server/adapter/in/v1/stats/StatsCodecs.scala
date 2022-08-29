package de.neuland.bandwhichd.server.adapter.in.v1.stats

import de.neuland.bandwhichd.server.domain.stats.*
import de.neuland.bandwhichd.server.lib.dot.*
import io.circe.Json

import java.util.concurrent.atomic.AtomicReference

object StatsCodecs {
  val circeEncoder: io.circe.Encoder[MonitoredStats] =
    (stats: MonitoredStats) =>
      Json.obj(
        "hosts" -> Json.fromFields(
          stats.hosts
            .map(monitoredHost =>
              monitoredHost.hostId.uuid.toString -> Json.obj(
                "hostname" -> Json.fromString(monitoredHost.hostname.toString),
                "additional_hostnames" -> Json.fromValues(
                  monitoredHost.additionalHostnames.map(additionalHostname =>
                    Json.fromString(additionalHostname.toString)
                  )
                ),
                "connections" -> stats
                  .connectionsFor(monitoredHost.hostId)
                  .fold(Json.obj())(hostIdsToConnections => {
                    Json.fromFields(
                      hostIdsToConnections.map[(String, Json)]((hostId, _) => {
                        hostId.uuid.toString -> Json.obj()
                      })
                    )
                  })
              )
            )
        ),
        "unmonitoredHosts" -> Json.fromFields(
          stats.unidentifiedRemoteHosts.map(unidentifiedRemoteHost => {
            unidentifiedRemoteHost.hostId.uuid.toString -> Json.obj(
              "host" -> Json.fromString(unidentifiedRemoteHost.host.toString)
            )
          })
        )
      )

  val dotEncoder
      : de.neuland.bandwhichd.server.lib.dot.codec.Encoder[MonitoredStats] =
    (stats: MonitoredStats) => {

      val nodeStatements: Seq[Node] =
        stats.allHosts.toSeq.map(host =>
          Node(
            id = NodeId(host.hostId.uuid.toString),
            attributes = Seq(
              Attribute.Label(host.host.toString)
            )
          )
        )
      val edgeStatements: Seq[Edge] =
        stats.connections.toSeq
          .map(connection =>
            Edge(
              idA = NodeId(connection._1.uuid.toString),
              idB = NodeId(connection._2.uuid.toString)
            )
          )

      Graph(
        `type` = Directed,
        statements = nodeStatements ++ edgeStatements
      )
    }
}

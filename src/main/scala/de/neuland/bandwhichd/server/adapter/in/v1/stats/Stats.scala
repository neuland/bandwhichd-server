package de.neuland.bandwhichd.server.adapter.in.v1.stats

import de.neuland.bandwhichd.server.domain.stats.{
  MonitoredHost,
  UnidentifiedHost
}
import de.neuland.bandwhichd.server.lib.dot.*
import io.circe.Json

import java.util.concurrent.atomic.AtomicReference

type Stats = de.neuland.bandwhichd.server.domain.stats.Stats

object Stats {
  val circeEncoder: io.circe.Encoder[Stats] =
    (stats: Stats) =>
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
                )
              )
            )
        )
      )

  val dotEncoder: de.neuland.bandwhichd.server.lib.dot.codec.Encoder[Stats] =
    (stats: Stats) => {

      val nodeStatements: Seq[Node] =
        stats.hosts.toSeq.map(host =>
          Node(
            id = NodeId(host.hostId.uuid.toString),
            attributes = Seq(
              Attribute.Label(host.hostname.toString)
            )
          )
        ) ++ stats.connections.flatMap(connection =>
          connection._2 match
            case unidentifiedHost: UnidentifiedHost =>
              Some(
                Node(
                  id = NodeId(unidentifiedHost.hostId.uuid.toString),
                  attributes = Seq(
                    Attribute.Label(unidentifiedHost.host.toString)
                  )
                )
              )
            case _ => None
        )
      val edgeStatements: Seq[Edge] = stats.connections.toSeq
        .map(connection =>
          Edge(
            idA = NodeId(connection._1.hostId.uuid.toString),
            idB = NodeId(connection._2.hostId.uuid.toString)
          )
        )

      Graph(
        `type` = Directed,
        statements = nodeStatements ++ edgeStatements
      )
    }
}

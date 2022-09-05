package de.neuland.bandwhichd.server.adapter.in.v1.stats

import de.neuland.bandwhichd.server.domain.stats.*
import io.circe.{Encoder, Json}

object StatsCodecs {
  val encoder: Encoder[MonitoredStats] =
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
}

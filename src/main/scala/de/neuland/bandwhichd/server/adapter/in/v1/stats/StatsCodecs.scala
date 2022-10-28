package de.neuland.bandwhichd.server.adapter.in.v1.stats

import de.neuland.bandwhichd.server.domain.stats.*
import io.circe.{Encoder, Json}

object StatsCodecs {
  val encoder: Encoder[MonitoredStats] =
    (stats: MonitoredStats) =>
      Json
        .obj(
          "hosts" -> Json.fromFields(
            stats.hosts
              .map(monitoredHost =>
                monitoredHost.hostId.uuid.toString -> Json.obj(
                  "hostname" -> Json.fromString(
                    monitoredHost.hostname.toString
                  ),
                  "os_release" -> monitoredHost.maybeOsRelease.fold(Json.Null)(
                    osRelease =>
                      Json.obj(
                        "pretty_name" -> osRelease.maybePrettyName
                          .fold(Json.Null)(prettyName =>
                            Json.fromString(prettyName.value)
                          ),
                        "version_id" -> osRelease.maybeVersionId
                          .fold(Json.Null)(versionId =>
                            Json.fromString(versionId.value)
                          ),
                        "id" -> osRelease.maybeId.fold(Json.Null)(id =>
                          Json.fromString(id.value)
                        )
                      )
                  ),
                  "additional_hostnames" -> Json.fromValues(
                    monitoredHost.additionalHostnames.map(additionalHostname =>
                      Json.fromString(additionalHostname.toString)
                    )
                  ),
                  "connections" -> stats
                    .connectionsFor(monitoredHost.hostId)
                    .fold(Json.obj())(hostIdsToConnections => {
                      Json.fromFields(
                        hostIdsToConnections
                          .map[(String, Json)]((hostId, _) => {
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
        .deepDropNullValues
}

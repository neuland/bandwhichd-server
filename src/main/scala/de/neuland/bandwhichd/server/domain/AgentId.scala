package de.neuland.bandwhichd.server.domain

import java.util.UUID

opaque type AgentId = UUID

object AgentId {
  def apply(value: UUID): AgentId = value

  extension (agentId: AgentId) {
    def value: UUID = agentId
  }
}

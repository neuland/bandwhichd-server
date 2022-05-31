package de.neuland.bandwhichd.server.domain.measurement

import com.comcast.ip4s.Hostname
import de.neuland.bandwhichd.server.domain.*

sealed trait Measurement[+T <: Timing] {
  def agentId: AgentId
  def timing: T
}

object Measurement {
  case class NetworkConfiguration(
      agentId: AgentId,
      timing: Timing.Timestamp,
      machineId: MachineId,
      hostname: Hostname,
      interfaces: Seq[Interface],
      openSockets: Seq[OpenSocket]
  ) extends Measurement[Timing.Timestamp]

  case class NetworkUtilization(
      agentId: AgentId,
      timing: Timing.Timeframe,
      connections: Seq[Connection]
  ) extends Measurement[Timing.Timeframe]
}

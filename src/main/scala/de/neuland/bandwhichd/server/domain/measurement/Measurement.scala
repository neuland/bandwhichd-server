package de.neuland.bandwhichd.server.domain.measurement

import com.comcast.ip4s.Hostname
import de.neuland.bandwhichd.server.domain.*

sealed trait Measurement[+T <: Timing] {
  def machineId: MachineId
  def timing: T
  def timestamp: Timing.Timestamp =
    timing match
      case timestamp: Timing.Timestamp => timestamp
      case timeframe: Timing.Timeframe => timeframe.start
}

object Measurement {
  case class NetworkConfiguration(
      machineId: MachineId,
      timing: Timing.Timestamp,
      hostname: Hostname,
      interfaces: Seq[Interface],
      openSockets: Seq[OpenSocket]
  ) extends Measurement[Timing.Timestamp]

  case class NetworkUtilization(
      machineId: MachineId,
      timing: Timing.Timeframe,
      connections: Seq[Connection]
  ) extends Measurement[Timing.Timeframe]
}

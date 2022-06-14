package de.neuland.bandwhichd.server.lib.health.jvm

import de.neuland.bandwhichd.server.lib.health.{Check, CheckKey, Status}
import io.circe.{Json, JsonObject}

/** @see https://stackoverflow.com/a/18375641 */
case class JvmMemoryUtilization(runtime: Runtime) extends Check {
  override def key: CheckKey = CheckKey("memory:utilization")
  override def value: JsonObject = JsonObject(
    "observedValue" -> Json.obj(
      "used_memory_percentage" -> Json.fromLong(usedMemoryPercentage),
      "used_memory_bytes" -> Json.fromLong(usedMemoryBytes),
      "max_memory_bytes" -> Json.fromLong(maxMemoryBytes)
    ),
    "status" -> Json.fromString(status.stringValue)
  )

  def maxMemoryBytes: Long =
    runtime.maxMemory()

  def allocatedMemoryBytes: Long =
    runtime.totalMemory()

  def freeAllocatedMemoryBytes: Long =
    runtime.freeMemory()

  def usedMemoryBytes: Long =
    allocatedMemoryBytes - freeAllocatedMemoryBytes

  def usedMemoryPercentage: Long =
    (100 * usedMemoryBytes) / maxMemoryBytes

  def unusedMemoryBytes: Long =
    maxMemoryBytes - usedMemoryBytes

  val warnThresholdPercentage: Long =
    95

  def isWarnThresholdReached: Boolean =
    usedMemoryPercentage > warnThresholdPercentage

  def status: Status =
    if (isWarnThresholdReached)
      Status.Warn
    else
      Status.Pass

  override def maybeStatus: Option[Status] =
    Some(status)
}

object JvmMemoryUtilization {
  val current: JvmMemoryUtilization = JvmMemoryUtilization(Runtime.getRuntime)
}

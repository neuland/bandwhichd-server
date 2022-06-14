package de.neuland.bandwhichd.server.lib.health

enum Status {
  case Pass, Fail, Warn

  def stringValue: String =
    this match
      case Pass => "pass"
      case Warn => "warn"
      case Fail => "fail"

  def severity: Int =
    this match
      case Pass => 0
      case Warn => 1
      case Fail => 2
}

object Status {
  def from(value: String): Option[Status] =
    value match
      case "pass" => Some(Pass)
      case "fail" => Some(Fail)
      case "warn" => Some(Warn)
      case _      => None

  implicit val ordering: Ordering[Status] =
    (x: Status, y: Status) => y.severity - x.severity
}

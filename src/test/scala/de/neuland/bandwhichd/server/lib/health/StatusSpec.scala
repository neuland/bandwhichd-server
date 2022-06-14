package de.neuland.bandwhichd.server.lib.health

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StatusSpec extends AnyWordSpec with Matchers {
  "Status" should {
    "have ordering" in {
      Status.Pass should be > Status.Warn
      Status.Pass should be > Status.Fail
      Status.Warn should be > Status.Fail
    }
  }
}

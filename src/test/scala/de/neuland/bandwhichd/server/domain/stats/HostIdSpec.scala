package de.neuland.bandwhichd.server.domain.stats

import com.comcast.ip4s.Hostname
import de.neuland.bandwhichd.server.domain.MachineId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class HostIdSpec extends AnyWordSpec with Matchers {
  "HostId" when {
    "derived from machine id" should {
      "have uuid from machine id" in {
        // given
        val uuid = UUID.fromString("6417a24d-589a-4850-a471-91f569cd2d09")
        val machineId: MachineId = MachineId(uuid)

        // when
        val result: HostId = HostId(machineId)

        // then
        result.uuid shouldBe uuid
      }
    }
  }
}

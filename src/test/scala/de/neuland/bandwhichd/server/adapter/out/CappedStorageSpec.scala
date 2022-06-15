package de.neuland.bandwhichd.server.adapter.out

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CappedStorageSpec extends AnyWordSpec with Matchers {
  "CappedStorage" should {
    "store and cap" in {
      val cappedStorage0 = CappedStorage.empty[Int]
      cappedStorage0.maybeCap shouldBe None
      cappedStorage0.storage shouldBe empty
      val cappedStorage1 = cappedStorage0.store(3)
      cappedStorage1.maybeCap shouldBe None
      cappedStorage1.storage shouldBe Vector(3)
      val cappedStorage2 = cappedStorage1.store(2)
      cappedStorage2.maybeCap shouldBe None
      cappedStorage2.storage shouldBe Vector(3, 2)
      val cappedStorage3 = cappedStorage2.store(-5)
      cappedStorage3.maybeCap shouldBe None
      cappedStorage3.storage shouldBe Vector(3, 2, -5)
      val cappedStorage4 = cappedStorage3.cap
      cappedStorage4.maybeCap shouldBe Some(3)
      cappedStorage4.storage shouldBe Vector(3, 2, -5)
      val cappedStorage5 = cappedStorage4.store(9)
      cappedStorage5.maybeCap shouldBe Some(3)
      cappedStorage5.storage shouldBe Vector(2, -5, 9)
      val cappedStorage6 = cappedStorage5.store(0)
      cappedStorage6.maybeCap shouldBe Some(3)
      cappedStorage6.storage shouldBe Vector(-5, 9, 0)
    }
  }
}

package de.neuland.bandwhichd.server.domain

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OsReleaseSpec extends AnyWordSpec with Matchers with OptionValues {

  private val debian11FileContents = OsRelease.FileContents(
    "PRETTY_NAME=\"Debian GNU/Linux 11 (bullseye)\"\nNAME=\"Debian GNU/Linux\"\nVERSION_ID=\"11\"\nVERSION=\"11 (bullseye)\"\nVERSION_CODENAME=bullseye\nID=debian\nHOME_URL=\"https://www.debian.org/\"\nSUPPORT_URL=\"https://www.debian.org/support\"\nBUG_REPORT_URL=\"https://bugs.debian.org/\""
  )

  "OsRelease" should {
    "be empty" when {
      "file contents are empty" in {
        // given
        val fileContents = OsRelease.FileContents("")

        // when
        val osRelease = OsRelease(fileContents = fileContents)

        // then
        osRelease.maybeId shouldBe empty
        osRelease.maybeVersionId shouldBe empty
        osRelease.maybePrettyName shouldBe empty
      }
    }

    "have ID" when {
      "file contents are from Debian GNU/Linux 11 (bullseye)" in {
        // when
        val osRelease = OsRelease(debian11FileContents)

        // then
        osRelease.maybeId.value shouldBe OsRelease.Id("debian")
      }
    }

    "have version ID" when {
      "file contents are from Debian GNU/Linux 11 (bullseye)" in {
        // when
        val osRelease = OsRelease(debian11FileContents)

        // then
        osRelease.maybeVersionId.value shouldBe OsRelease.VersionId("11")
      }
    }

    "have pretty name" when {
      "file contents are from Debian GNU/Linux 11 (bullseye)" in {
        // when
        val osRelease = OsRelease(debian11FileContents)

        // then
        osRelease.maybePrettyName.value shouldBe OsRelease.PrettyName(
          "Debian GNU/Linux 11 (bullseye)"
        )
      }
    }
  }
}

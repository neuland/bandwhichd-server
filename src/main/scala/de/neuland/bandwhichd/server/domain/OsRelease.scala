package de.neuland.bandwhichd.server.domain

import scala.util.matching.Regex

case class OsRelease(
    maybeId: Option[OsRelease.Id],
    maybeVersionId: Option[OsRelease.VersionId],
    maybePrettyName: Option[OsRelease.PrettyName]
)

object OsRelease {
  def apply(fileContents: FileContents): OsRelease = {
    import de.neuland.bandwhichd.server.domain.OsRelease.FileContents.findValue

    OsRelease(
      maybeId = fileContents.findValue("ID").map(Id.apply),
      maybeVersionId =
        fileContents.findValue("VERSION_ID").map(VersionId.apply),
      maybePrettyName =
        fileContents.findValue("PRETTY_NAME").map(PrettyName.apply)
    )
  }

  opaque type FileContents = String

  object FileContents {
    def apply(value: String): FileContents = value

    private val rowRegex =
      """^ *([a-zA-Z]+[a-zA-Z0-9_]*) *= *(?:"([^"]*)"|([a-zA-Z0-9]+)) *$""".r

    extension (fileContents: FileContents) {
      def value: String = fileContents

      def parse: OsRelease = OsRelease.apply(fileContents)

      def findValue(key: String): Option[String] =
        fileContents.value
          .split("\\n")
          .to(LazyList)
          .flatMap(rowRegex.findFirstMatchIn)
          .flatMap(_ match
            case Regex.Groups(foundKey, quotedValue, null)
                if key.equalsIgnoreCase(foundKey) =>
              Some(quotedValue)
            case Regex.Groups(foundKey, null, unquotedValue)
                if key.equalsIgnoreCase(foundKey) =>
              Some(unquotedValue)
            case _ => None
          )
          .headOption
    }
  }

  opaque type Id = String

  object Id {
    def apply(value: String): Id = value

    extension (id: Id) {
      def value: String = id
    }
  }

  opaque type VersionId = String

  object VersionId {
    def apply(value: String): VersionId = value

    extension (versionId: VersionId) {
      def value: String = versionId
    }
  }

  opaque type PrettyName = String

  object PrettyName {
    def apply(value: String): PrettyName = value

    extension (prettyName: PrettyName) {
      def value: String = prettyName
    }
  }
}

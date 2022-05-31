package de.neuland.bandwhichd.server.domain

opaque type BytesCount = BigInt

object BytesCount {
  def apply(value: BigInt): BytesCount = value

  extension (bytesCount: BytesCount) {
    def value: BigInt = bytesCount
  }
}

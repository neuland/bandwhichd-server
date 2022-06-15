package de.neuland.bandwhichd.server.adapter.out

case class CappedStorage[A] private (
    maybeCap: Option[Int],
    storage: Vector[A]
) {
  def cap: CappedStorage[A] =
    capAt(storage.length)

  private def capAt(cap: Int): CappedStorage[A] =
    capAt(Some(cap))

  private def capAt(maybeCap: Option[Int]): CappedStorage[A] =
    maybeCap.fold(CappedStorage(maybeCap, storage))(cap =>
      CappedStorage(maybeCap, storage.drop(storage.length - cap))
    )

  def store(value: A): CappedStorage[A] =
    CappedStorage(
      maybeCap,
      maybeCap
        .fold(storage)(cap => storage.drop(storage.length + 1 - cap))
        .appended(value)
    )
}

object CappedStorage {
  def empty[A]: CappedStorage[A] = CappedStorage(None, Vector.empty)
}

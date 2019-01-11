package com.github.oskin1.macaw

import java.util.concurrent.atomic.AtomicLong

abstract sealed class TreeVector[+A] extends Serializable {

  /** Number of elements in this vector.
    */
  def size: Int

  /** Alias for [[size]].
    */
  final def length: Int = size

  /** Returns true if this vector contains no elements.
    */
  final def isEmpty: Boolean = size == 0

  /** Returns true if this vector contains non-zero number of elements.
    */
  final def nonEmpty: Boolean = !isEmpty

  /** Returns the element at specified `idx`.
    * @throws IndexOutOfBoundsException if the specified index is not in `[0, size)`
    */
  def get(idx: Int): A = {
    validIndex(idx)
    get0(idx)
  }

  /** Alias for [[get]].
    * @throws IndexOutOfBoundsException if the specified index is not in `[0, size)`
    */
  final def apply(idx: Int): A = get(idx)

  /** Returns the element at specified `idx` or [[None]] if `idx` is out of range.
    */
  final def lift(idx: Int): Option[A] = if (idx >= 0 && idx < size) Some(get(idx)) else None

  /** Returns vector with the element at the specified `idx` replaced by the specified element `elt`.
    */
  final def update[B >: A](idx: Int, elt: B): TreeVector[B] = ???

  /** Returns vector with the specified element `elt` inserted at the specified `idx`.
    */
  final def insert[B >: A](idx: Int, elt: B): TreeVector[B] = ???

  /** Returns vector containing current vector contents followed by the `other`s vector contents.
    */
  def ++[B >: A](other: TreeVector[B]): TreeVector[B] = ???

  /** Return vector with the specified element `elt` prepended.
    */
  def +:[B >: A](elt: B): TreeVector[B] = ???

  /** Return vector with the specified element `elt` appended.
    */
  def :+[B >: A](elt: B): TreeVector[B] = ???

  /** Returns vector containing all elements except first `n`.
    */
  def drop(n: Int): TreeVector[A] = ???

  /** Returns vector containing first `n` elements.
    */
  def take(n: Int): TreeVector[A] = ???

  protected def get0(idx: Int): A

  private def validIndex(idx: Int): Unit =
    if (idx < 0 || idx >= size) throw new IndexOutOfBoundsException(s"Invalid index: $idx for vector of size: $size")

}

object TreeVector {

  private[macaw] sealed abstract class At[+A] {
    def apply(idx: Int): A
  }

  private object AtEmpty extends At[Nothing] {
    override def apply(idx: Int) = throw new IllegalArgumentException("At empty view")
  }

  private class AtArray[A](val arr: Array[A]) extends At[A] {
    override def apply(idx: Int): A = arr(idx)
  }

  private[macaw] case class View[A](at: At[A], offset: Int, size: Int) {
    def apply(n: Int): A = at(offset + n)
  }

  private[macaw] object View {
    def empty[T]: View[T] = View[T](AtEmpty, 0, 0)
  }

  private[macaw] case class Chunk[A](elts: View[A]) extends TreeVector[A] {
    override def size: Int = elts.size
    override def get0(idx: Int): A = elts(idx)
  }

  private[macaw] case class Node[A](left: TreeVector[A], right: TreeVector[A]) extends TreeVector[A] {
    override def size: Int = left.size + right.size
    override def get0(idx: Int): A = if (idx < left.size) left.get0(idx) else right.get0(idx - left.size)
  }

  private[macaw] case class Chunks[A](chunks: Node[A]) extends TreeVector[A] {
    override def size: Int = chunks.size
    override protected def get0(idx: Int): A = chunks.get0(idx)
  }

  /** [[TreeVector]] supporting fast `:+` and `++` operations, using an (unobservable) mutable
    * scratch array at the end of the vector.
    */
  private[macaw] case class Buffer[A](id: AtomicLong,
                                      stamp: Long,
                                      hd: TreeVector[A],
                                      lastChunk: Array[A],
                                      lastSize: Int) extends TreeVector[A] {

    override def size: Int = hd.size + lastSize

    override def get0(idx: Int): A = if (idx < hd.size) hd.get0(idx) else lastChunk(idx - hd.size)
  }

  def empty[T]: TreeVector[T] = Chunk[T](View.empty)

}

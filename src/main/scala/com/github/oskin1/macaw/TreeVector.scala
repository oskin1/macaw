package com.github.oskin1.macaw

import java.util.concurrent.atomic.AtomicLong

import com.github.oskin1.macaw.TreeVector.{Buffer, Chunk, Chunks, Concat}

import scala.reflect.ClassTag

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

  /** Returns the element at specified `index`.
    * @throws IndexOutOfBoundsException if the specified index is not in `[0, size)`
    */
  def get(index: Int): A = {
    validIndex(index)
    get0(index)
  }

  /** Alias for [[get]].
    * @throws IndexOutOfBoundsException if the specified index is not in `[0, size)`
    */
  final def apply(index: Int): A = get(index)

  /** Returns the element at specified `index` or [[None]] if `index` is out of range.
    */
  final def lift(index: Int): Option[A] = if (index >= 0 && index < size) Some(get(index)) else None

  /** Returns vector with the element at the specified `index` replaced by the specified element `elem`.
    */
  final def update[B >: A](index: Int, elem: B): TreeVector[B] = (take(index) :+ elem) ++ drop(index + 1)

  /** Returns vector with the specified element `elem` inserted at the specified `index`.
    */
  final def insert[B >: A](index: Int, elem: B): TreeVector[B] = (take(index) :+ elem) ++ drop(index)

  /** Returns vector containing current vector contents followed by the `other`s vector contents.
    */
  def ++[B >: A](other: TreeVector[B]): TreeVector[B] = if (isEmpty) other else Chunks(Concat(this, other)).bufferBy(64)

  /** Return vector with the specified element `elem` prepended.
    */
  def +:[B >: A](elem: B): TreeVector[B] = TreeVector(elem) ++ this

  /** Return vector with the specified element `elem` appended.
    */
  def :+[B >: A](elem: B): TreeVector[B] = this ++ TreeVector(elem)

  /** Returns vector containing all elements except first `n`.
    */
  def drop(n: Int): TreeVector[A] = {
    val rn = n min size max 0
    if (rn == size) TreeVector.empty
    else if (rn == 0) this
    else {
      def loop(cur: TreeVector[A], rn: Int, accR: List[TreeVector[A]]): TreeVector[A] = cur match {
        case Chunk(elems) => accR.foldLeft(Chunk(elems.drop(rn)): TreeVector[A])(_ ++ _)
        case Concat(left, right) =>
          if (rn > left.size) loop(right, rn - left.size, accR) else loop(left, rn, right :: accR)
        case bf: Buffer[A@unchecked] =>
          if (rn > bf.hd.size) loop(bf.lastElems, rn - bf.hd.size, accR) else loop(bf.hd, rn, bf.lastElems :: accR)
        case Chunks(chunks) => loop(chunks, rn, accR)
      }
      loop(this, rn, Nil)
    }
  }

  /** Returns vector containing first `n` elements.
    */
  def take(n: Int): TreeVector[A] = {
    val rn = n min size max 0
    if (rn == size) this
    else if (rn == 0) TreeVector.empty
    else {
      def loop(cur: TreeVector[A], rn: Int, accL: TreeVector[A]): TreeVector[A] = cur match {
        case Chunk(elems) => accL ++ Chunk(elems.take(rn))
        case Concat(left, right) =>
          if (rn > left.size) loop(right, rn - left.size, accL ++ left) else loop(left, rn, accL)
        case bf: Buffer[A@unchecked] => loop(bf.unbuffer, rn, accL)
        case Chunks(chunks) => loop(chunks, rn, accL)
      }
      loop(this, rn, TreeVector.empty)
    }
  }

  /** Allocate mutable scratch space with chunks of the specified size at the end of this vector.
    * Note, that `:+`, `++` and `drop` on the result of call to `buffer` return another buffered vector.
    */
  final def bufferBy(chunkSize: Int): TreeVector[A] = this match {
    case bf: Buffer[A@unchecked] => if (bf.lastChunk.length >= chunkSize) bf else bf.rebuffer(chunkSize)
    case _ => Buffer(new AtomicLong(0), 0, this, new Array[A](chunkSize), 0)
  }

  /** Collapse any buffered chunks at the end of this vector in an unbuffered vector.
    */
  def unbuffer: TreeVector[A] = this

  protected def get0(index: Int): A

  private def validIndex(index: Int): Unit =
    if (index < 0 || index >= size) throw new IndexOutOfBoundsException(s"Invalid index: $index for vector of size: $size")

}

object TreeVector {

  private[macaw] sealed abstract class At[+A] {
    def apply(index: Int): A
  }

  private object AtEmpty extends At[Nothing] {
    override def apply(index: Int) = throw new IllegalArgumentException("At empty view")
  }

  private class AtArray[A](val arr: Array[A]) extends At[A] {
    override def apply(index: Int): A = arr(index)
  }

  private[macaw] case class View[A](at: At[A], offset: Int, size: Int) {
    def apply(n: Int): A = at(offset + n)
    def drop(n: Int): View[A] = ???
    def take(n: Int): View[A] = ???
  }

  private[macaw] object View {
    def empty[T]: View[T] = View[T](AtEmpty, 0, 0)
  }

  private[macaw] case class Chunk[A](elems: View[A]) extends TreeVector[A] {
    override def size: Int = elems.size
    override def get0(index: Int): A = elems(index)
  }

  private[macaw] case class Concat[A](left: TreeVector[A], right: TreeVector[A]) extends TreeVector[A] {
    override def size: Int = left.size + right.size
    override def get0(index: Int): A = if (index < left.size) left.get0(index) else right.get0(index - left.size)
  }

  private[macaw] case class Chunks[A](chunks: Concat[A]) extends TreeVector[A] {
    override def size: Int = chunks.size
    override protected def get0(index: Int): A = chunks.get0(index)
  }

  /** Supports fast `:+` and `++` operations using an unobservable mutable
    * scratch array at the end of the vector.
    */
  private[macaw] case class Buffer[A](id: AtomicLong,
                                      stamp: Long,
                                      hd: TreeVector[A],
                                      lastChunk: Array[A],
                                      lastSize: Int) extends TreeVector[A] {

    override def size: Int = hd.size + lastSize

    override def get0(index: Int): A = if (index < hd.size) hd.get0(index) else lastChunk(index - hd.size)

    def lastElems: TreeVector[A] = ???

    def rebuffer(chunkSize: Int): TreeVector[A] = ???

  }

  def apply[T](elems: T*): TreeVector[T] = ???

  def empty[T]: TreeVector[T] = Chunk[T](View.empty)

  def view[T](elems: Array[T]): TreeVector[T] = Chunk(View(new AtArray(elems), 0, elems.length))

}

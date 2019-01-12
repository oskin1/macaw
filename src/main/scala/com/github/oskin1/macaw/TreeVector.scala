package com.github.oskin1.macaw

import java.util.concurrent.atomic.AtomicLong

import com.github.oskin1.macaw.TreeVector._

import scala.collection.mutable.ArrayBuffer

abstract sealed class TreeVector[A] extends Serializable {

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
  final def update(index: Int, elem: A): TreeVector[A] = (take(index) :+ elem) ++ drop(index + 1)

  /** Returns vector with the specified element `elem` inserted at the specified `index`.
    */
  final def insert(index: Int, elem: A): TreeVector[A] = (take(index) :+ elem) ++ drop(index)

  /** Returns vector containing current vector contents followed by the `other`s vector contents.
    */
  def ++(other: TreeVector[A]): TreeVector[A] = if (isEmpty) other else Chunks(Concat(this, other)).bufferBy(64)

  /** Return vector with the specified element `elem` prepended.
    */
  def +:(elem: A): TreeVector[A] = TreeVector(elem) ++ this

  /** Return vector with the specified element `elem` appended.
    */
  def :+(elem: A): TreeVector[A] = this ++ TreeVector(elem)

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
    case _ => Buffer(new AtomicLong(0), 0, this, new ArrayBuffer[A](chunkSize), 0)
  }

  /** Collapse any buffered chunks at the end of this vector in an unbuffered vector.
    */
  def unbuffer: TreeVector[A] = this

  def toArrayBuffer: ArrayBuffer[A] = {
    val bf = new ArrayBuffer[A](size)
    copyToBuffer(bf, 0)
    bf
  }

  final def copy: TreeVector[A] = Chunk(View(new AtArray(this.toArrayBuffer), 0, size))

  final def copyToBuffer(bf: ArrayBuffer[A], start: Int): Unit = {
    var i = start
    foreachV { v => v.copyToBuffer(bf, i); i += v.size }
  }

  protected def get0(index: Int): A

  private final def foreachV(f: View[A] => Unit): Unit = {
    def loop(rem: List[TreeVector[A]]): Unit = rem match {
      case Chunk(elems) :: tail => f(elems); loop(tail)
      case Concat(left, right) :: tail => loop(left :: right :: tail)
      case Chunks(Concat(left, right)) :: tail => loop(left :: right :: tail)
      case (bf: Buffer[A@unchecked]) :: tail => loop(bf.unbuffer :: tail)
      case Nil => ()
    }
    loop(this :: Nil)
  }

  private def validIndex(index: Int): Unit =
    if (index < 0 || index >= size) throw new IndexOutOfBoundsException(s"Invalid index: $index for vector of size: $size")

}

object TreeVector {

  private[macaw] sealed abstract class At[+A] {
    def apply(index: Int): A
    def copyToBuffer[B >: A](bf: ArrayBuffer[B], offset: Int, size: Int): Unit = ???
  }

  private object AtEmpty extends At[Nothing] {
    override def apply(index: Int) = throw new IllegalArgumentException("At empty view")
  }

  private class AtArray[A](val arr: ArrayBuffer[A]) extends At[A] {
    override def apply(index: Int): A = arr(index)
  }

  private[macaw] case class View[A](at: At[A], offset: Int, size: Int) {
    def apply(n: Int): A = at(offset + n)
    def drop(n: Int): View[A] = ???
    def take(n: Int): View[A] = ???
    def copyToBuffer(bf: ArrayBuffer[A], start: Int): Unit = ???
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
    * scratch array buffer at the end of the vector.
    */
  private[macaw] case class Buffer[A](id: AtomicLong,
                                      stamp: Long,
                                      hd: TreeVector[A],
                                      lastChunk: ArrayBuffer[A],
                                      lastSize: Int) extends TreeVector[A] {

    override def size: Int = hd.size + lastSize

    override def take(n: Int): TreeVector[A] =
      if (n <= hd.size) hd.take(n)
      else hd ++ lastElems.take(n - hd.size)

    override def drop(n: Int): TreeVector[A] =
      if (n <= hd.size) Buffer(id, stamp, hd.drop(n), lastChunk, lastSize)
      else unbuffer.drop(n).bufferBy(lastChunk.length)

    override def :+(elem: A): TreeVector[A] =
      if (id.compareAndSet(stamp, stamp + 1) && lastSize < lastChunk.length) { // treads race to update buffer mutably.
        lastChunk(lastSize) = elem
        Buffer(id, stamp + 1, hd, lastChunk, lastSize + 1)
      } else { // loser has to copy scratch space.
        scratchSpaceCopy :+ elem
      }

    // if other vector fits in scratch space and is itself is a buffer then it will be unbuffered
    // before being added in order to avoid proliferation of scratch space for small vectors.
    override def ++(other: TreeVector[A]): TreeVector[A] =
      if (other.isEmpty) this
      else {
        if (id.compareAndSet(stamp, stamp + 1) && (lastChunk.length - lastSize > other.size)) {
          other.copyToBuffer(lastChunk, lastSize)
          Buffer(id, stamp + 1, hd, lastChunk, lastSize + other.size)
        } else {
          if (lastSize == 0) Buffer(id, stamp, (hd ++ other).unbuffer, lastChunk, lastSize)
          else scratchSpaceCopy ++ other
        }
      }

    override def get0(index: Int): A =
      if (index < hd.size) hd.get0(index)
      else lastChunk(index - hd.size)

    override def unbuffer: TreeVector[A] = {
      // copy last chunk to a new vector if it is more than half unused to avoid proliferation of scratch space
      val restElems = if (lastSize * 2 < lastChunk.length) lastElems.copy else lastElems
      hd ++ restElems
    }

    def rebuffer(chunkSize: Int): TreeVector[A] = {
      require(chunkSize > lastChunk.length)
      val newChunk = new ArrayBuffer[A](chunkSize)
      lastChunk.copyToBuffer(newChunk)
      Buffer(new AtomicLong(0), 0, hd, newChunk, lastSize)
    }

    def lastElems: TreeVector[A] = TreeVector.view(lastChunk).take(lastSize)

    private def scratchSpaceCopy = Buffer(new AtomicLong(0), 0, unbuffer, new ArrayBuffer[A](lastChunk.length), 0)

  }

  def apply[T](elems: T*): TreeVector[T] = {
    val bf = new ArrayBuffer[T](elems.size)
    var i = 0
    elems.foreach { e =>
      bf(i) = e
      i += 1
    }
    view(bf)
  }

  def empty[T]: TreeVector[T] = Chunk[T](View.empty)

  def view[T](elems: ArrayBuffer[T]): TreeVector[T] = Chunk(View(new AtArray(elems), 0, elems.length))

}

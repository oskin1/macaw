package com.github.oskin1.macaw.core

import cats.ApplicativeError
import cats.syntax.applicativeError._

/** A type class allowing to signal business errors of type `E`.
  */
trait Raise[F[_], -E] {

  def raise[A](e: E): F[A]
}

object Raise {

  trait Infallible[F[_]] extends Raise[F, Nothing]

  object Infallible {

    implicit def instance[F[_]]: Infallible[F] =
      new Infallible[F] {
        override def raise[A](e: Nothing): F[A] = e
      }
  }

  def apply[F[_], E](
    implicit ev: Raise[F, E]
  ): Raise[F, E] = ev

  implicit def instance[
    F[_]: ApplicativeError[*[_], Throwable],
    E <: Throwable
  ]: Raise[F, E] =
    new Raise[F, E] {
      def raise[A](e: E): F[A] = e.raiseError
    }

  object syntax {

    implicit class RaiseOps[
      F[_]: Raise[*[_], E],
      E
    ](e: E) {
      def raise[A]: F[A] = Raise[F, E].raise[A](e)
    }
  }
}

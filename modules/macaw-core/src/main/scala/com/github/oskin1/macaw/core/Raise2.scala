package com.github.oskin1.macaw.core

import cats.ApplicativeError
import cats.mtl.ApplicativeHandle

/** A type class allowing to signal business errors of type `E`.
  */
trait Raise2[F[+ _, + _], E <: Throwable] {

  def raise[A](e: E): F[E, A]
}

object Raise2 {

  def apply[F[+ _, + _], E <: Throwable](
    implicit ev: Raise2[F, E]
  ): Raise2[F, E] = ev

  implicit def instance0[
    F[+ _, + _],
    E <: Throwable
  ](implicit F: ApplicativeError[F[E, *], Throwable]): Raise2[F, E] =
    new Raise2[F, E] {
      def raise[A](e: E): F[E, A] = F.raiseError(e)
    }

  implicit def instance[
    F[+ _, + _],
    E <: Throwable
  ](implicit F: ApplicativeHandle[F[E, *], Throwable]): Raise2[F, E] =
    new Raise2[F, E] {
      def raise[A](e: E): F[E, A] = F.raise(e)
    }

  object syntax {

    implicit class RaiseOps[
      F[+ _, + _],
      E <: Throwable
    ](e: E)(implicit F: Raise2[F, E]) {
      def raise[A](e: E): F[E, A] = Raise2[F, E].raise[A](e)
    }
  }
}

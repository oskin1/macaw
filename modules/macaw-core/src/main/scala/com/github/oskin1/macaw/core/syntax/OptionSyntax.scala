package com.github.oskin1.macaw.core.syntax

import cats.Applicative
import com.github.oskin1.macaw.core.{Raise, Raise2}
import com.github.oskin1.macaw.core.syntax.OptionSyntax.OptionOps

trait OptionSyntax {

  implicit final def toOptionOps[A](oa: Option[A]): OptionOps[A] =
    new OptionOps(oa)
}

object OptionSyntax {

  final private[syntax] class OptionOps[A](private val oa: Option[A]) extends AnyVal {

    def liftTo[F[_]] = new LiftToPartiallyApplied[F, A](oa)

    def liftTo2[
      F[+ _, + _],
      E <: Throwable
    ](ifEmpty: => E)(
      implicit
      F: Raise2[F, E],
      A: Applicative[F[E, *]]
    ): F[E, A] =
      oa match {
        case Some(value) => A.pure(value)
        case None        => F.raise[A](ifEmpty)
      }
  }

  final private[syntax] class LiftToPartiallyApplied[F[_], A](oa: Option[A]) {

    def apply[E](ifEmpty: => E)(
      implicit
      F: Raise[F, _ >: E],
      A: Applicative[F]
    ): F[A] =
      oa match {
        case Some(value) => A.pure(value)
        case None        => F.raise(ifEmpty)
      }
  }
}

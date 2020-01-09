package com.github.oskin1.macaw.core.syntax

import cats.Applicative
import com.github.oskin1.macaw.core.Raise
import com.github.oskin1.macaw.core.syntax.EitherSyntax.EitherOps

trait EitherSyntax {

  implicit final def toEitherOps[E, A](either: E Either A): EitherOps[E, A] =
    new EitherOps(either)
}

object EitherSyntax {

  final private[syntax] class EitherOps[E, A](either: E Either A) {

    def liftToRaise[F[_]](
      implicit
      F: Raise[F, _ >: E],
      A: Applicative[F]
    ): F[A] =
      either match {
        case Right(value) => A.pure(value)
        case Left(e)      => F.raise(e)
      }
  }
}

package com.github.oskin1.macaw.core.syntax

import com.github.oskin1.macaw.core.Raise
import com.github.oskin1.macaw.core.syntax.RaiseSyntax.RaiseOps

trait RaiseSyntax {

  implicit final def toRaiseOps[E](e: E): RaiseOps[E] =
    new RaiseOps(e)
}

object RaiseSyntax {

  final private[syntax] class RaiseOps[E](private val e: E) extends AnyVal {

    def raise[F[_], A](implicit F: Raise[F, _ >: E]): F[A] =
      F.raise(e)
  }
}

package com.github.oskin1.macaw.core.syntax

import com.github.oskin1.macaw.core.Raise2
import com.github.oskin1.macaw.core.syntax.Raise2Syntax.Raise2Ops

trait Raise2Syntax {

  implicit final def toRaise2Ops[E <: Throwable](e: E): Raise2Ops[E] =
    new Raise2Ops(e)
}

object Raise2Syntax {

  final private[syntax] class Raise2Ops[E <: Throwable](
    private val e: E
  ) extends AnyVal {

    def raise[F[+ _, + _], A](implicit F: Raise2[F, E]): F[E, A] =
      F.raise(e)
  }
}

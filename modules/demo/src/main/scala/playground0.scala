import cats.Applicative
import cats.mtl.FunctorRaise
import cats.syntax.option._
import com.github.oskin1.macaw.core.Raise2
import zio.DefaultRuntime
import zio.interop.catz._
import com.github.oskin1.macaw.core.syntax.option._

object playground0 extends App {

  val runtime = new DefaultRuntime {}

  abstract class Err(m: String) extends Exception(m)
  final case class BusinessErr(msg: String) extends Err(msg)
  abstract class ErrSubtype(m: String) extends Err(m)
  final case class BusinessErrSub(msg: String) extends ErrSubtype(msg)

  trait MyService[F[+ _, + _]] {

    def method0: F[Err, Int]

    def method1: F[Err, Int]
  }

  final class MyServiceImpl[F[+ _, + _]](
    implicit
    F: Raise2[F, Err],
    A: Applicative[F[Err, *]]
  ) extends MyService[F] {

    def method0: F[Err, Int] =
      none[Int].liftTo2[F, Err](BusinessErr("Boom!"))

    def method1: F[Err, Int] =
      Raise2[F, Err].raise(BusinessErrSub("Boom!"))
  }

  implicit def apHandleIO: FunctorRaise[zio.IO[Err, *], Err] =
    mtl.zioApplicativeHandle[Any, Err]

  val service = new MyServiceImpl[zio.IO]

  runtime.unsafeRun(service.method0.catchAll { e =>
    zio.IO.effectTotal(println(s"Caught: ${e.getMessage}"))
  })
}

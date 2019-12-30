import cats.Applicative
import cats.mtl.ApplicativeHandle
import cats.syntax.option._
import com.github.oskin1.macaw.core.Raise2
import com.github.oskin1.macaw.core.syntax.option._
import zio.DefaultRuntime
import zio.interop.catz._

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
      none[Int].liftTo2[F, Err](BusinessErr("BusinessErr!"))

    def method1: F[Err, Int] =
      Raise2[F, Err].raise(BusinessErrSub("BusinessErrSub!"))
  }

  implicit def apHandleIO: ApplicativeHandle[zio.IO[Err, *], Throwable] =
    mtl.zioApplicativeHandle[Any, Throwable]
      .asInstanceOf[ApplicativeHandle[zio.IO[Err, *], Throwable]]

  implicit def raiseIO: Raise2[zio.IO, Err] =
    Raise2.instance[zio.IO, Err](apHandleIO)

  val service = new MyServiceImpl[zio.IO]

  runtime.unsafeRun(service.method0.catchAll { e =>
    zio.IO.effectTotal(println(s"Caught: ${e.getMessage}"))
  })
  runtime.unsafeRun(service.method1.catchAll { e =>
    zio.IO.effectTotal(println(s"Caught: ${e.getMessage}"))
  })
}

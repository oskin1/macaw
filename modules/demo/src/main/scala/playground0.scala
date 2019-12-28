import cats.mtl.FunctorRaise
import com.github.oskin1.macaw.core.Raise2
import zio.DefaultRuntime
import zio.interop.catz._

object playground0 extends App {

  val runtime = new DefaultRuntime {}

  abstract class Err(m: String) extends Exception(m)
  final case class BusinessErr(msg: String) extends Err(msg)

  trait MyService[D[+ _, + _]] {

    def fn0: D[Err, Int]
  }

  final class MyServiceImpl[D[+ _, + _]: Raise2[*[_, _], Err]]
    extends MyService[D] {

    def fn0: D[Err, Int] =
      Raise2[D, Err].raise(BusinessErr("Boom!"))
  }

  implicit def apHandleIO: FunctorRaise[zio.IO[Err, *], Err] =
    mtl.zioApplicativeHandle[Any, Err]

  val service = new MyServiceImpl[zio.IO]

  runtime.unsafeRun(service.fn0.catchAll { e =>
    zio.IO.effectTotal(println(s"Caught: ${e.getMessage}"))
  })
}

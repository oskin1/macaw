import cats.effect.{ExitCase, IO, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, ApplicativeError, Functor, Monad}
import com.github.oskin1.macaw.core.Raise
import com.github.oskin1.macaw.core.Raise.Infallible
import com.olegpy.meow.hierarchy._

object playground1 extends App {

  trait Err extends Exception

  object Err {
    trait DbErr extends Err

    object DbErr {
      case class InconsistentData(msg: String) extends DbErr
      case class EmptyUserData(name: String) extends DbErr
    }
    case class BadCardFormat(msg: String) extends Err

    implicit def handler[F[_]](
      implicit F: ApplicativeError[F, Err]
    ): DummyAppErrorHandler[F, Err] =
      new DummyAppErrorHandler[F, Err] {
        override def handle[A](fa: F[A]): F[ExitCase[Err]] =
          F.attempt(fa).map(_.fold(ExitCase.error, _ => ExitCase.complete))
      }
  }

  // Repos

  abstract class MyRepo[F[_]: Raise[*[_], E], E] {

    def getUsername(id: Int): F[String]

    def getUserIdByCard(card: String): F[Int]
  }

  class MyRepoImpl[F[_]: Raise[*[_], Err.DbErr]] extends MyRepo[F, Err.DbErr] {

    def getUsername(id: Int): F[String] =
      Raise[F, Err.DbErr].raise(Err.DbErr.InconsistentData("Wrong user data"))

    def getUserIdByCard(card: String): F[Int] =
      Raise[F, Err.DbErr]
        .raise(Err.DbErr.EmptyUserData(s"No card for user with card: $card"))
  }

  class MyRepoInfallibleImpl[F[_]: Infallible: Applicative] extends MyRepo[F, Nothing] {

    def getUsername(id: Int): F[String] =
      Applicative[F].pure(s"User$id")

    def getUserIdByCard(card: String): F[Int] =
      Applicative[F].pure(0)
  }

  // Services

  abstract class MyService[F[_]: Raise[*[_], E], E] {

    def getFirstUser: F[String]

    def getUsernameByCard(card: String): F[String]
  }

  class MyServiceImpl[F[_]: Raise[*[_], Err]: Applicative](repo: MyRepo[F, _ <: Err])
    extends MyService[F, Err] {

    def getFirstUser: F[String] =
      repo.getUsername(0)

    def getUsernameByCard(card: String): F[String] =
      Raise[F, Err].raise(Err.BadCardFormat(card))
  }

  class MyServiceInfallibleImpl[F[_]: Infallible: Monad](repo: MyRepo[F, Nothing])
    extends MyService[F, Nothing] {

    def getFirstUser: F[String] =
      repo.getUsername(0)

    def getUsernameByCard(card: String): F[String] =
      repo.getUserIdByCard(card) >>= repo.getUsername
  }

  class MyApp[F[_]: Sync, E](service: MyService[F, E])(
    implicit H: DummyAppErrorHandler[F, E]
  ) {

    def run: F[ExitCase[E]] =
      H.handle(program).flatTap(res => Sync[F].delay(println(res)))

    private def program: F[Unit] =
      service.getFirstUser >> service.getUsernameByCard("1234").void
  }

  val app           = new MyApp[IO, Err](new MyServiceImpl[IO](new MyRepoImpl[IO]))
  val infallibleApp = new MyApp[IO, Nothing](new MyServiceInfallibleImpl[IO](new MyRepoInfallibleImpl[IO]))

  app.run.unsafeRunSync()
  infallibleApp.run.unsafeRunSync()

  trait DummyAppErrorHandler[F[_], E] {
    def handle[A](fa: F[A]): F[ExitCase[E]]
  }

  object DummyAppErrorHandler {

    trait InfallibleHandler[F[_]] extends DummyAppErrorHandler[F, Nothing]

    implicit def instance[F[_]: Functor]: InfallibleHandler[F] =
      new InfallibleHandler[F] {
        override def handle[A](fa: F[A]): F[ExitCase[Nothing]] =
          Functor[F].fmap(fa)(_ => ExitCase.complete)
      }
  }
}

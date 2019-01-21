package services.db

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabasePublisher
import slick.dbio.{DBIOAction, NoStream, Streaming}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

@ImplementedBy(classOf[DBServiceImpl])
trait DBService {
  def runAsync[R](a: DBIOAction[R, NoStream, Nothing]): Future[R]

  def runStream[T](a: DBIOAction[_, Streaming[T], Nothing]): DatabasePublisher[T]

  //  def run[R](a: DBIOAction[R, NoStream, Nothing]): R
}

@Singleton
class DBServiceImpl @Inject()(val dbConfigProvider: DatabaseConfigProvider) extends DBService {
  private val db = dbConfigProvider.get[JdbcProfile].db

  def runAsync[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = {
    db.run(a)
  }

  def runStream[T](a: DBIOAction[_, Streaming[T], Nothing]): DatabasePublisher[T] = {
    db.stream(a)
  }

  /*
  def run[R](a: DBIOAction[R, NoStream, Nothing]): R = {
    Await.result(runAsync(a), Duration.Inf)
  }
  */
}

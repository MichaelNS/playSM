import com.typesafe.config.{Config, ConfigFactory}
import controllers.{SmCategory, SmCategoryView}
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec, ServerProvider}
import play.api.Logger
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.CSRFTokenHelper._

//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.util.{Failure, Success}


/**
  * Runs a browser test using Fluentium against a play application on a server port.
  */
class SmCategorySpec extends PlaySpec
  with OneBrowserPerTest
  with GuiceOneServerPerTest
  with HtmlUnitFactory
  with ServerProvider
  with BeforeAndAfter {

  private val logger = Logger(classOf[SmCategorySpec])

  val config: Config = ConfigFactory.load("application.conf")

  val driver = "org.h2.Driver"
  val url = "jdbc:h2:mem:play_test;DATABASE_TO_UPPER=false;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
  val username: String = config.getString("slick.dbs.default.db.user")
  val password: String = config.getString("slick.dbs.default.db.password")

  val flyway: Flyway = Flyway
    .configure()
    .dataSource(url, username, password)
    .locations("db/migration/default")
    .load()

  // fix - org.flywaydb.core.api.FlywayException: Found non-empty schema(s) "PUBLIC" without schema history table! Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
  flyway.baseline()

  logger.debug(url)
  logger.debug(username)
  logger.debug(password)
  flyway.getConfiguration.getLocations.foreach(q => logger.info(q.toString))

  flyway.migrate()


  before {

  }

  after {

  }

  // SmCategory ---------------------------------------------------------------------------------------------------------
  "UserController SmCategory" should {

    "run listCategoryAndCnt" in {
      val controller = app.injector.instanceOf[SmCategoryView]
      val request = FakeRequest().withCSRFToken
      val result = controller.listCategoryAndCnt.apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "run listSubCategoryAndCnt" in {
      val controller = app.injector.instanceOf[SmCategoryView]
      val request = FakeRequest()
      val result = controller.listSubCategoryAndCnt("").apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "run listDescriptionAndCnt" in {
      val controller = app.injector.instanceOf[SmCategoryView]
      val request = FakeRequest()
      val result = controller.listDescriptionAndCnt("", "").apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "run listDirWithoutCatByLastDate" in {
      val controller = app.injector.instanceOf[SmCategoryView]
      val request = FakeRequest()
      val result = controller.listDirWithoutCatByLastDate.apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "run listFcWithoutCatByLastDate" in {
      val controller = app.injector.instanceOf[SmCategoryView]
      val request = FakeRequest()
      val result = controller.listFcWithoutCatByLastDate.apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }



    // listDirWithoutCatByParent
    "run listDirWithoutCatByParent - isBegins = true" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.listDirWithoutCatByParent("", isBegins = true).apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
    "run listDirWithoutCatByParent" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.listDirWithoutCatByParent("").apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }


    "run listDirWithoutCategoryByExtension" in {
      val controller = app.injector.instanceOf[SmCategoryView]
      val request = FakeRequest()
        .withFormUrlEncodedBody("extension" -> "")
        .withCSRFToken
      val result = controller.listDirWithoutCategoryByExtension().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
    "run listDirWithoutCategoryByExtension NON empty" in {
      val controller = app.injector.instanceOf[SmCategoryView]
      val request = FakeRequest()
        .withFormUrlEncodedBody("extension" -> "jpg")
        .withCSRFToken
      val result = controller.listDirWithoutCategoryByExtension().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "run assignCategoryAndDescription" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.assignCategoryAndDescription("").apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/html")
    }

    // getStreamFcByParent
    "run getStreamFcByParent - isBegins = true" in {
      val controller = app.injector.instanceOf[SmCategory]
      controller.getStreamFcByParent("", isBegins = true)
    }
    "run getStreamFcByParent" in {
      val controller = app.injector.instanceOf[SmCategory]
      controller.getStreamFcByParent("")
    }

    "run writeToCategoryTbl" in {
      val controller = app.injector.instanceOf[SmCategory]
      val message: (Option[String], String) = (Some("sha_id"), "fileName")

      controller.writeToCategoryTbl(message, "categoryType", "category", "subCategory", "description")
      //      val result = controller.writeToCategoryTbl(message, "categoryType", "description")
      //      result.onComplete {
      //        case Success(insSuc) => logger.warn(s"Upsert cat = $insSuc")
      //        case Failure(t) => logger.error(s"An error has occured (Upsert cat): = ${t.getMessage}")
      //      }
      //      Thread.sleep(1300)
    }
  }
}

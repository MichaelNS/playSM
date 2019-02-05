import com.github.tototoshi.fixture._
import controllers.SmCategory
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec, ServerProvider}
import play.api.Logger
import play.api.test.FakeRequest
import play.api.test.Helpers._

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

  val driver = "org.h2.Driver"
  val url = "jdbc:h2:mem:play_test;DATABASE_TO_UPPER=false;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
  val username = "play_user"
  val password = "1234"

  val flyway = new Flyway()
  flyway.setDataSource(url, username, password)
  flyway.setLocations("db/migration/default")

  // fix - org.flywaydb.core.api.FlywayException: Found non-empty schema(s) "PUBLIC" without schema history table! Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
  flyway.setBaselineOnMigrate(true)
  //  flyway.baseline()
  flyway.setSchemas("public")

  logger.debug(url)
  logger.debug(username)
  logger.debug(password)
  flyway.getLocations.foreach(q => logger.debug(q.toString))

  flyway.migrate()

  val fixture: Fixture = Fixture(driver, url, username, password)
    .scriptLocation("db/fixtures/default")
    .scriptPackage("ru.ns.fixtures")
    .scripts(Seq("sm_device.sql", "sm_file_card.sql", "sm_path_move.sql", "MyFixtureScript"))

  before {
    fixture.setUp()
  }

  after {
    fixture.tearDown()
  }

  // SmCategory ---------------------------------------------------------------------------------------------------------
  "UserController SmCategory" should {

    "run listCategoryAndCnt" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.listCategoryAndCnt.apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "run listDescriptionAndCnt" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.listDescriptionAndCnt("").apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "run listFcWithoutCatByLastDate" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.listFcWithoutCatByLastDate.apply(request)

      //      status(result) mustBe OK
      //      contentType(result) mustBe Some("text/html")
    }



    // listDirWithoutCatByParent
    "run listDirWithoutCatByParent - isBegins = true" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.listDirWithoutCatByParent("", isBegins = true).apply(request)

      //      status(result) mustBe OK
      //      contentType(result) mustBe Some("text/html")
    }
    "run listDirWithoutCatByParent" in {
      val controller = app.injector.instanceOf[SmCategory]
      val request = FakeRequest()
      val result = controller.listDirWithoutCatByParent("").apply(request)

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

      controller.writeToCategoryTbl(message, "categoryType", "description")
      //      val result = controller.writeToCategoryTbl(message, "categoryType", "description")
      //      result.onComplete {
      //        case Success(insSuc) => logger.warn(s"Upsert cat = $insSuc")
      //        case Failure(t) => logger.error(s"An error has occured (Upsert cat): = ${t.getMessage}")
      //      }
      //      Thread.sleep(1300)
    }
  }
}

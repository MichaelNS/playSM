import better.files.File
import com.github.tototoshi.fixture._
import controllers.{SmApplication, SmMove, SmSync}
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec, ServerProvider}
import play.api.Logger
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import ru.ns.model.OsConf

/**
  * Runs a browser test using Fluentium against a play application on a server port.
  */
class BrowserSpec extends PlaySpec
  with OneBrowserPerTest
  with GuiceOneServerPerTest
  with HtmlUnitFactory
  with ServerProvider
  with BeforeAndAfter {

  private val logger = Logger(classOf[BrowserSpec])

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


  // SmApplication ---------------------------------------------------------------------------------------------------

  "Application" should {
    "work from within a browser" in {
      go to ("http://localhost:" + port)
      pageSource must include("Storage Manager")
    }
  }

  "UserController GET" should {
    "render the index page from the application" in {
      val controller = app.injector.instanceOf[SmApplication]
      val request = FakeRequest().withCSRFToken
      val result = controller.smIndex().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
  }

  // SmMove ---------------------------------------------------------------------------------------------------------
  "UserController SmMove" should {
    "run moveByDevice" in {
      import ru.ns.model.Device

      val device: Device = Device(name = "name", label = "label", uuid = "111-222", mountpoint = "/tmp", fstype = "ext4")
      val controller = app.injector.instanceOf[SmMove]
      controller.moveByDevice(device, 3, 10)
    }

    "render the moveAllDevices" in {
      val controller = app.injector.instanceOf[SmMove]
      val request = FakeRequest().withCSRFToken
      val result = controller.moveAllDevices().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/plain")
    }

    "render the listPathByDescription" in {
      val controller = app.injector.instanceOf[SmMove]
      val request = FakeRequest().withCSRFToken
      val result = controller.listPathByDescription("Project", "Some play").apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "render the createJobToMove" in {
      val controller = app.injector.instanceOf[SmMove]
      val request = FakeRequest().withCSRFToken
      val result = controller.createJobToMove(categoryType = "1", description = "2", device = "deviceID", oldPath = "Downloads/html").apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/html")
    }

    "render the delJobToMove" in {
      val controller = app.injector.instanceOf[SmMove]
      val request = FakeRequest().withCSRFToken
      val result = controller.delJobToMove(categoryType = "", description = "", device = "", path = "").apply(request)

      status(result) mustBe SEE_OTHER
      contentType(result) mustBe None
    }

    "run clearJob" in {
      val dir_clearJob: File = File.newTemporaryDirectory("test-play-sm-tst-clearJob-")
      dir_clearJob.deleteOnExit()

      val idJob: Int = -2
      val storeName: String = "a"

      var fParent: Option[String] = None
      var mountPoint: Option[String] = None

      if (OsConf.isWindows) {
        val pathStr: String = dir_clearJob.path.toString
        fParent = Some(pathStr.drop(3)) // drop "c:\"
        mountPoint = Some(pathStr.take(2)) // take "c:"
      }
      else if (OsConf.isUnix) {
        fParent = Some(dir_clearJob.path.toString.drop(1))
        mountPoint = Some(OsConf.fsSeparator)
      }
      fParent.isDefined mustBe true
      mountPoint.isDefined mustBe true

      val controller = app.injector.instanceOf[SmMove]

      val res = controller.clearJob(idJob = idJob, storeName = storeName, mountPoint = mountPoint.get, pathFrom = fParent.get)

      res mustBe "clearJob is DONE"
      Thread.sleep(300) // for DB delete record
      dir_clearJob.exists mustBe false
    }
  }

  // SmSync ---------------------------------------------------------------------------------------------------------
  "UserController refresh device" should {
    "render the refresh device" in {
      val controller = app.injector.instanceOf[SmSync]
      val request = FakeRequest().withCSRFToken
      val result = controller.refreshDevice().apply(request)

      status(result) mustBe SEE_OTHER
      contentType(result) mustBe None
    }
  }
}

import better.files.File
import com.typesafe.config.{Config, ConfigFactory}
import controllers._
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

  val config: Config = ConfigFactory.load("application.conf")

  val driver: String = config.getString("slick.dbs.default.db.profile")
  val url: String = config.getString("slick.dbs.default.db.url")
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

    "render the listPathByCategory" in {
      val controller = app.injector.instanceOf[SmMove]
      val request = FakeRequest().withCSRFToken
      val result = controller.listPathByCategory("Project", "Src", "Some play").apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }

    "render the createJobToMove" in {
      val controller = app.injector.instanceOf[SmMove]
      val request = FakeRequest().withCSRFToken
      val result = controller.createJobToMove(categoryType = "1", category = "2", subCategory = "3", device = "deviceID", oldPath = "Downloads/html").apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("text/html")
    }

    "render the delJobToMove" in {
      val controller = app.injector.instanceOf[SmMove]
      val request = FakeRequest().withCSRFToken
      val result = controller.delJobToMove(categoryType = "", category = "", subCategory = "", device = "", path = "").apply(request)

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

      val res = controller.closeJob(idJob = idJob, storeName = storeName, mountPoint = mountPoint.get, pathFrom = fParent.get)

      res mustBe "clearJob is DONE"
      Thread.sleep(300) // for DB delete record
      dir_clearJob.exists mustBe false
    }
  }

  // SmSyncDeviceStream -----------------------------------------------------------------------------------------------
  "UserController import device" should {
    "render the import device" in {
      val controller = app.injector.instanceOf[SmSyncDeviceStream]
      val request = FakeRequest().withCSRFToken
      val result = controller.importDevice().apply(request)

      status(result) mustBe SEE_OTHER
      contentType(result) mustBe None
    }
  }
  "UserController deviceImport" should {
    "render the deviceImport" in {
      val controller = app.injector.instanceOf[SmSyncDeviceStream]
      val request = FakeRequest().withCSRFToken
      val result = controller.deviceImport().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
  }

  // SmReport ---------------------------------------------------------------------------------------------------------
  "UserController checkBackAllFiles" should {
    "render the checkBackAllFiles" in {
      val controller = app.injector.instanceOf[SmReport]
      val request = FakeRequest().withCSRFToken
      val result = controller.checkBackAllFiles().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
  }

  "UserController checkBackFilesLastYear" should {
      "render the checkBackFilesLastYear" in {
        val controller = app.injector.instanceOf[SmReport]
        val request = FakeRequest().withCSRFToken
        val result = controller.checkBackFilesLastYear().apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
      }
    }

  "UserController explorerDevice" should {
      "render the explorerDevice" in {
        val controller = app.injector.instanceOf[SmView]
        val request = FakeRequest().withCSRFToken
        val result = controller.explorerDevice("","","",1).apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
      }
    }

  // SmSearch ---------------------------------------------------------------------------------------------------------
  "UserController byFileName" should {
    "render the byFileName" in {
      val controller = app.injector.instanceOf[SmSearch]
      val request = FakeRequest().withCSRFToken
      val result = controller.byFileName("", 1).apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }
  }

  // SmView ---------------------------------------------------------------------------------------------------------
  "UserController viewStorage" should {
    "render the viewStorage" in {
      val controller = app.injector.instanceOf[SmView]
      val request = FakeRequest().withCSRFToken
      val result = controller.viewStorage("", 1).apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
  }

  "UserController viewPathBySha256" should {
    "render the viewPathBySha256" in {
      val controller = app.injector.instanceOf[SmView]
      val request = FakeRequest().withCSRFToken
      val result = controller.viewPathBySha256("").apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
  }

}

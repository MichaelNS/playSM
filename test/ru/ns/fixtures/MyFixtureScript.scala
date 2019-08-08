package ru.ns.fixtures

import java.sql.Connection

import better.files.File
import com.github.tototoshi.fixture.FixtureScript
import ru.ns.model.Device
import ru.ns.tools.FileUtils

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

class MyFixtureScript extends FixtureScript {

  val dir_root: File = File.newTemporaryDirectory("test-play-sm-root-")
  dir_root.deleteOnExit()

  val dir_from: File = File.newTemporaryDirectory("test-play-sm-tst-1-", Some(dir_root))
  dir_from.deleteOnExit()

  val dir_to: File = File.newTemporaryDirectory("test-play-sm-tst-2-", Some(dir_root))
  dir_to.deleteOnExit()

  val file_1: File = File.newTemporaryFile("test-play-sm-", "-tst_1", Some(dir_from))
  file_1.deleteOnExit()

  val devUid: String = "111-222"


  val lstDevices: ArrayBuffer[Device] = Await.result(FileUtils.getDevicesInfo(), 15.seconds)
  val device: Device = lstDevices.head

  override def setUp(connection: Connection): Unit = {
    connection
      .prepareStatement("INSERT INTO sm_path_move (ID, STORE_NAME, PATH_FROM, PATH_TO) " +
        "VALUES ('1', '" + devUid + "', '" + dir_from.toString + "', '" + dir_to.toString + "');")
      .execute()

    connection.prepareStatement("" +
      "INSERT INTO sm_file_card (ID, STORE_NAME, F_PARENT, " +
      "F_NAME, F_EXTENSION, F_CREATION_DATE, F_LAST_MODIFIED_DATE, F_SIZE, F_MIME_TYPE_JAVA, " +
      "SHA256, F_NAME_LC)" +

      "VALUES ('qwe-asd', '" + devUid + "', '" + dir_from.toString + "', " +
      "'" + file_1.toString + "', '', '2017-05-25 12:34:47.000000', '2015-08-23 13:42:04.000000', '140', '', " +
      "'ADD5487EFD4FD4186CC350B66EF35AAE89FF6752', '" + file_1.toString.toLowerCase() + "')" +
      ";").execute()

    // add real device for test SmSync.refreshDevice
    connection.prepareStatement("INSERT INTO SM_DEVICE (ID, NAME, LABEL, UID, SYNC_DATE) " +
      "VALUES ('2', '" + device.name + "', '" + device.label + "', '" + device.uuid + "', '2015-08-23 13:42:04.000000' );")
      .execute()
  }

  override def tearDown(connection: Connection): Unit = {
    connection.prepareStatement("DELETE FROM sm_path_move where id > 0").execute()

    connection.prepareStatement("DELETE FROM sm_file_card where id = 'qwe-asd';").execute()

    connection.prepareStatement("DELETE FROM SM_DEVICE where id > 0").execute()
  }
}

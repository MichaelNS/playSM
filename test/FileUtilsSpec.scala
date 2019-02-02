import java.io.{File, FileNotFoundException, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file.FileStore
import java.nio.file.attribute.{FileAttributeView, FileStoreAttributeView}
import java.time.ZoneId
import java.{lang, util}

import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.scalatestplus.play.PlaySpec
import ru.ns.model.{Device, FileCardSt, OsConf, SmPath}
import ru.ns.tools.FileUtils

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

class FileUtilsSpec extends PlaySpec {

  "getNixDevicesInfo is OK" in {

    implicit val cmdProc: String =
      """
        |{
        |   "blockdevices": [
        |      {"name": "loop1", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/keepassxc/26"},
        |      {"name": "loop8", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/remmina/343"},
        |      {"name": "loop6", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/remmina/310"},
        |      {"name": "loop4", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/keepassxc/25"},
        |      {"name": "loop2", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/postgresql96/33"},
        |      {"name": "loop0", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/core/3247"},
        |      {"name": "sda", "fstype": null, "label": null, "uuid": null, "mountpoint": null,
        |         "children": [
        |            {"name": "sda2", "fstype": "ext4", "label": null, "uuid": "e97dc14f-a56c-40c4-864a-c21670596a1b", "mountpoint": "/"},
        |            {"name": "sda3", "fstype": "swap", "label": null, "uuid": "fffb5659-d57f-4bab-ae2b-94c8a0a73291", "mountpoint": "[SWAP]"},
        |            {"name": "sda1", "fstype": "vfat", "label": null, "uuid": "C221-6C72", "mountpoint": "/boot/efi"}
        |         ]
        |      },
        |      {"name": "loop7", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/core/3017"},
        |      {"name": "loop5", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/sensors-unity/29"},
        |      {"name": "loop3", "fstype": "squashfs", "label": null, "uuid": null, "mountpoint": "/snap/remmina/333"}
        |   ]
        |}
        |
        """.stripMargin

    val res = FileUtils.getNixDevicesInfo

    res.size mustBe 3

    val dev_1: Device = Device("sda2", "", "e97dc14f-a56c-40c4-864a-c21670596a1b", "/", "ext4")
    val dev_2: Device = Device("sda3", "", "fffb5659-d57f-4bab-ae2b-94c8a0a73291", "[SWAP]", "swap")
    val dev_3: Device = Device("sda1", "", "C221-6C72", "/boot/efi", "vfat")

    val lstDevices = Vector[Device](dev_1, dev_2, dev_3)
    res mustBe lstDevices
  }

  "getWinDevicesInfo is OK" in {
    val fs1: FileStore = new FileStore {
      override def getUsableSpace: Long = ???

      override def `type`(): String = ???

      override def getTotalSpace: Long = ???

      override def isReadOnly: Boolean = ???

      override def getAttribute(attribute: String): AnyRef = ???

      override def supportsFileAttributeView(`type`: Class[_ <: FileAttributeView]): Boolean = ???

      override def supportsFileAttributeView(name: String): Boolean = ???

      override def name(): String = "PLEX"

      def winLetter(): String = "(C:)"

      override def getUnallocatedSpace: Long = ???

      override def getFileStoreAttributeView[V <: FileStoreAttributeView](`type`: Class[V]): V = ???

      override def toString(): String = s"${name()} ${winLetter()}"
    }

    val dev_1: Device = Device(name = "", label = "PLEX", uuid = "PLEX", mountpoint = "C:", fstype = "")
    val lstDevices = ArrayBuffer[Device](dev_1)

    implicit val fileStores: lang.Iterable[FileStore] = util.Arrays.asList(fs1)
    val res = FileUtils.getWinDevicesInfo
    res.size mustBe 1

    res mustBe lstDevices


  }

  "check crc file is equals" in {
    val fName = "./test/tmp/test1.txt"
    val hash = Files.asByteSource(new File(fName)).hash(Hashing.sha256)

    val crc = FileUtils.getGuavaSha256(fName)
    assert(crc != "")
    crc mustBe hash.toString.toUpperCase
  }

  "throw FileNotFoundException if file not exist" in {
    a[FileNotFoundException] should be thrownBy {
      FileUtils.getGuavaSha256("file_not_exist")
    }
  }
  /*
      ignore("throw IOException with lock file") {
        val fileName = "./test/tmp/test1.txt"
        import java.io.RandomAccessFile
        val raFile = new RandomAccessFile(fileName, "rw")
        raFile.getChannel.lock

        a[IOException] should be thrownBy {
          FileUtils.getGuavaSha256(fileName)
        }
      }
  */
  "throw IOException with '' parameter FileName" in {
    a[IOException] should be thrownBy {
      FileUtils.getGuavaSha256("")
    }
  }

  "readDirRecursive return files" in {
    import java.nio.file.attribute.BasicFileAttributes
    import java.nio.file.{Files, Paths}

    val file_1 = Paths.get("./test/tmp/test1.txt")
    val attrs_1 = Files.readAttributes(file_1, classOf[BasicFileAttributes])

    file_1.toFile.exists mustBe true

    val fParent = "test" + OsConf.fsSeparator + "tmp"
    val deviceUid = "B6D40831D407F283"
    var mountPoint = Paths.get(fParent).toFile.getAbsolutePath.replace(fParent, "")
    mountPoint = mountPoint.substring(0, mountPoint.length() - 1)

    val path_1 = SmPath("test/tmp/")

    val fName1 = "test1.txt"
    val fc_1 = FileCardSt(
      id = Hashing.sha256().hashString(deviceUid + fParent + OsConf.fsSeparator + fName1, StandardCharsets.UTF_8).toString.toUpperCase,
      storeName = deviceUid,
      fParent = fParent + "/",
      fName = fName1,
      fExtension = Some("txt"),
      fCreationDate = java.time.LocalDateTime.ofInstant(attrs_1.creationTime.toInstant, ZoneId.systemDefault()),
      fLastModifiedDate = java.time.LocalDateTime.ofInstant(attrs_1.lastModifiedTime().toInstant, ZoneId.systemDefault()),
      fSize = Some(attrs_1.size),
      fMimeTypeJava = Some("text/plain"),
      fNameLc = fName1.toLowerCase
    )

    val file_2 = Paths.get("./test/tmp/test2.txt")
    val attrs_2 = Files.readAttributes(file_2, classOf[BasicFileAttributes])
    file_2.toFile.exists mustBe true
    val fName2 = "test2.txt"

    val fc_2 = FileCardSt(
      id = Hashing.sha256().hashString(deviceUid + fParent + OsConf.fsSeparator + fName2, StandardCharsets.UTF_8).toString.toUpperCase,
      storeName = deviceUid,
      fParent = fParent + "/",
      fName = fName2,
      fExtension = Some("txt"),
      fCreationDate = java.time.LocalDateTime.ofInstant(attrs_2.creationTime.toInstant, ZoneId.systemDefault()),
      fLastModifiedDate = java.time.LocalDateTime.ofInstant(attrs_2.lastModifiedTime().toInstant, ZoneId.systemDefault()),
      fSize = Some(attrs_2.size),
      fMimeTypeJava = Some("text/plain"),
      fNameLc = fName2.toLowerCase
    )

    val sExclusionDir: List[String] = List("")
    val sExclusionFile: List[String] = List("")

    val hSmBoSmPath: ArrayBuffer[SmPath] = FileUtils.getPathesRecursive(
      "test" + OsConf.fsSeparator + "tmp",
      mountPoint,
      sExclusionDir.asJava
    )
    val hSmPathTest = ArrayBuffer[SmPath](path_1)
    hSmBoSmPath.size mustBe 1
    hSmBoSmPath.head.toString must equal(hSmPathTest.head.toString)


    val hSmBoFileCardTest = ArrayBuffer[FileCardSt](fc_1, fc_2)
    val hSmBoFileCard: ArrayBuffer[FileCardSt] = FileUtils.getFilesFromStore(
      "test" + OsConf.fsSeparator + "tmp",
      deviceUid,
      mountPoint,
      sExclusionFile.asJava
    )
    hSmBoFileCard.size mustBe 2
    hSmBoFileCard.head.toString must equal(hSmBoFileCardTest.head.toString)
    hSmBoFileCard.take(2).toString must equal(hSmBoFileCardTest.take(2).toString)
    hSmBoFileCard mustBe hSmBoFileCardTest
  }

}

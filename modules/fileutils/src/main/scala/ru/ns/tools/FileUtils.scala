package ru.ns.tools

import java.io.{FileNotFoundException, IOException}
import java.nio.file._
import java.util.regex.Pattern
import java.{lang, util}

import com.typesafe.scalalogging._
import org.slf4j.LoggerFactory
import ru.ns.model.{Device, FileCardSt, OsConf, SmPath}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by ns on 03.02.2017.
  */
object FileUtils {
  private val logger: Logger = Logger(LoggerFactory.getLogger(this.getClass))

  def debugParam(implicit line: sourcecode.Line, enclosing: sourcecode.Enclosing, args: sourcecode.Args): Unit = {
    logger.debug(s"debugParam ${enclosing.value} : ${line.value}  - "
      + args.value.map(_.map(a => a.source + s"=[${a.value}]").mkString("(", "\n, ", ")")).mkString("")
    )
  }

  def debug[V](value: sourcecode.Text[V])(implicit fullName: sourcecode.FullName): Unit = {
    logger.debug(s"${fullName.value} = ${value.source} : [${value.value}]")
  }

  def getDevicesInfo(deviceUid: String = ""): Future[ArrayBuffer[Device]] = Future[ArrayBuffer[Device]] {
    if (OsConf.isUnix) {
      import scala.sys.process._
      implicit val cmdProc: String = Seq("lsblk", "-Jf").!!

      if (deviceUid == "") getNixDevicesInfo else getNixDevicesInfo.filter(_.uuid == deviceUid)
    } else {
      implicit val fileStores: lang.Iterable[FileStore] = FileSystems.getDefault.getFileStores

      if (deviceUid == "") getWinDevicesInfo else getWinDevicesInfo.filter(_.uuid == deviceUid)
    }
  }

  def getNixDevicesInfo(implicit cmdProc: String): ArrayBuffer[Device] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._

    lazy val json = parse(cmdProc)

    val result: List[Product with Serializable] = for {
      JObject(child) <- json
      JField("name", JString(name)) <- child
      JField("label", jv) <- child
      JField("uuid", JString(uuid)) <- child
      JField("mountpoint", JString(mountpoint)) <- child
      JField("fstype", JString(fstype)) <- child
    } yield {
      jv match {
        case JString(label) => Device(name, label, uuid, mountpoint, fstype)
        case JNull => Device(name, "", uuid, mountpoint, fstype)
        case _ => None
      }
    }
    val devices: ArrayBuffer[Device] = ArrayBuffer[Device]()

    result.foreach { q =>
      devices += Device(q.productElement(0).toString, q.productElement(1).toString, q.productElement(2).toString,
        q.productElement(3).toString, q.productElement(4).toString
      )
    }

    debug(devices)
    devices
  }

  def getWinDevicesInfo(implicit fileStores: lang.Iterable[FileStore]): ArrayBuffer[Device] = {
    val devices: ArrayBuffer[Device] = ArrayBuffer[Device]()

    debug(fileStores)

    import scala.jdk.CollectionConverters._

    val lstAll = fileStores.asScala.map(x => x.name()).toSeq.filter(_ != "")
    val lstDist = lstAll.distinct

    if (lstAll == lstDist) {
      val pp = Pattern.compile("\\(([^)]+)\\)")

      for (cFileStore <- fileStores.asScala.filter(_.name() != "")) {

        val m = pp.matcher(cFileStore.toString)
        if (m.find) {
          devices += Device(name = "", label = cFileStore.name, uuid = cFileStore.name, mountpoint = m.group(1), fstype = "")
        } else {
          logger.error(s"Unable to determine the volume label = ${cFileStore.toString}")
        }
      }
    }

    devices
  }

  /**
    * return list child path of path2scan
    *
    * used [[SmSyncDeviceStream.foreachPath]]
    *
    * @param path2scan     paths to scan - (home/user/Documents)
    * @param mountPoint    mountPoint
    * @param sExclusionDir list of dir which need exclusion
    * @return ArrayBuffer[SmPath]
    */

  def getPathesRecursive(path2scan: String,
                         mountPoint: String,
                         sExclusionDir: util.List[String]
                        ): ArrayBuffer[SmPath] = {
    debug(path2scan)

    val visitor = new SmPathVisitor("", mountPoint, sExclusionDir)
    val startingDir = Paths.get(mountPoint + OsConf.fsSeparator + path2scan)

    if (startingDir.toFile.exists) {
      //      logger.debug(s"readDirRecursive2 -> Current path [$impPath]   startingDir [$startingDir]")

      try {
        Files.walkFileTree(startingDir, visitor)
      } catch {
        case ex: IOException =>
          logger.error(s"readDirRecursive2 error: ${ex.toString}\nStackTrace:\n${ex.getStackTrace.mkString("\n")}")
          throw ex
      }
    } else {
      logger.warn(s"readDirRecursive -> Current path IS NOT EXISTS [$path2scan]   startingDir [$startingDir]")
    }

    visitor.done()
  }

  /**
    * return list directories by path
    *
    * used [[SmSyncCmpDir]]
    *
    * @param path2scan  paths to scan - (home/user/Documents)
    * @param mountPoint mountPoint
    * @return ArrayBuffer[SmPath]
    */

  def getPathChildren(path2scan: String,
                      mountPoint: String,
                      maxDepth: Int = 1
                         ): (ArrayBuffer[SmPath], ArrayBuffer[SmPath]) = {
    debug(path2scan)

    val visitor = new SmRootVisitor("", mountPoint)
    val startingDir = Paths.get(mountPoint + OsConf.fsSeparator + path2scan)

    debug(startingDir)

    if (startingDir.toFile.exists) {
      try
        Files.walkFileTree(startingDir, util.EnumSet.noneOf(classOf[FileVisitOption]), maxDepth, visitor)
      catch {
        case ex: IOException =>
          logger.error(s"getPathChildren error: ${ex.toString}\nStackTrace:\n${ex.getStackTrace.mkString("\n")}")
          throw ex
      }
    } else {
      logger.warn(s"getPathChildren -> Current path IS NOT EXISTS [$path2scan]   startingDir [$startingDir]")
    }

    visitor.done()
  }

  def getFilesFromStore(impPath: String,
                        deviceUid: String,
                        mountPoint: String,
                        sExclusionFile: util.List[String]): ArrayBuffer[FileCardSt] = {

    val visitor = new SmFileVisitor("", deviceUid, mountPoint, sExclusionFile)

    val startingDir = Paths.get(mountPoint + OsConf.fsSeparator + impPath)

    if (startingDir.toFile.exists) {
      //      logger.debug(s"getFilesFromStore -> Current path [$impPath]   startingDir [$startingDir]")
      try
        Files.walkFileTree(startingDir, util.EnumSet.noneOf(classOf[FileVisitOption]), 1, visitor)
      catch {
        case ex: IOException =>
          logger.error(s"getFilesFromStore error: ${ex.toString}\nStackTrace:\n${ex.getStackTrace.mkString("\n")}")
          throw ex
      }
    } else {
      logger.warn(s"getFilesFromStore -> Current path IS NOT EXISTS [$impPath]   startingDir [$startingDir]")
    }

    visitor.done()
  }

  @throws[FileNotFoundException]
  @throws[IOException]
  def getGuavaSha256(fileName: String): String = {
    import java.io.{File, FileNotFoundException, IOException}

    import com.google.common.hash.Hashing
    import com.google.common.io.Files

    var shaCalc = ""
    try {
      val file = new File(fileName)
      val hash = Files.asByteSource(file).hash(Hashing.sha256())

      shaCalc = hash.toString.toUpperCase
    } catch {
      case ex: FileNotFoundException => // Handle missing file
        logger.error(s"getGuavaSha256 = ${ex.toString}")
        throw ex
      case ex: IOException => // Handle other I/O error
        logger.error(s"getGuavaSha256 = ${ex.toString}")
        throw ex
    }
    shaCalc
  }
}

package ru.ns.tools

import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.util

import com.google.common.hash.Hashing
import ru.ns.model.{FileCardSt, OsConf}

import scala.collection.mutable.ArrayBuffer

/**
  *
  * @param glob           glob
  * @param deviceUid      device Uid
  * @param mountPoint     mount point
  * @param sExclusionFile list of ExclusionFile
  */
class SmFileVisitor(glob: String, deviceUid: String, mountPoint: String, sExclusionFile: Seq[String])
  extends SimpleFileVisitor[Path] {

  //  private val logger: Logger = Logger(LoggerFactory.getLogger(this.getClass))

  val fileCardSt_lst: ArrayBuffer[FileCardSt] = ArrayBuffer[FileCardSt]()
  val pathMatcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:" + glob)
  val lstExclusionDir: util.TreeSet[String] = new util.TreeSet[String]
  val lstExclusionFullPath: util.TreeSet[String] = new util.TreeSet[String]
  var hDevise: Map[String, String] = Map[String, String]()

  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    if (sExclusionFile.contains(file.getFileName.toString) || file.toFile.isDirectory) {
      return FileVisitResult.CONTINUE
    }
    else {
      val i = file.getFileName.toString.lastIndexOf('.')
      if (i > 0) {
        // TODO remove hardcode ext 'class'
        if (file.getFileName.toString.substring(i + 1) == "class") {
          return FileVisitResult.CONTINUE
        }
      }
    }

    val fileName = file.getFileName.toString
    val extPos: Int = fileName.lastIndexOf('.')

    // TODO check performance fParent
    val fParent: String =
      if (mountPoint == OsConf.fsSeparator) {
        file.getParent.toUri.getPath.drop(1)
      } else {
        if (OsConf.isWindows) {
          file.getParent.toUri.getPath.substring(mountPoint.length).drop(2)
        } else {
          file.getParent.toUri.getPath.substring(mountPoint.length).drop(1)
        }
      }

    val fileCardSt: FileCardSt = FileCardSt(
      id = Hashing.sha256().hashString(deviceUid + fParent + fileName, StandardCharsets.UTF_8).toString.toUpperCase,
      deviceUid = deviceUid,
      fParent = fParent,
      fName = fileName,
      fExtension = if (extPos > 0) Some(fileName.substring(extPos + 1)) else None,
      fCreationDate = java.time.LocalDateTime.ofInstant(attrs.creationTime.toInstant, ZoneId.systemDefault()),
      fLastModifiedDate = java.time.LocalDateTime.ofInstant(attrs.lastModifiedTime.toInstant, ZoneId.systemDefault()),
      fSize = Some(attrs.size),
      fMimeTypeJava = Some(Files.probeContentType(file)),
      fNameLc = fileName.toLowerCase
    )

    fileCardSt_lst += fileCardSt
    FileVisitResult.CONTINUE
  }

  def done(): ArrayBuffer[FileCardSt] = {
    fileCardSt_lst
  }
}

package ru.ns.tools

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.util

import ru.ns.model.{FileCardSt, OsConf}

import scala.collection.mutable.ArrayBuffer


class SmMatchingVisitor(glob: String, deviceUid: String, mountPoint: String, sExclusionDir: util.List[String], sExclusionFile: util.List[String])
  extends SimpleFileVisitor[Path] {

  val fileCardSt_lst: ArrayBuffer[FileCardSt] = ArrayBuffer[FileCardSt]()
  val pathMatcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:" + glob)
  val lstExclusionDir: util.TreeSet[String] = new util.TreeSet[String]
  val lstExclusionFullPath: util.TreeSet[String] = new util.TreeSet[String]
  var hDevise: Map[String, String] = Map[String, String]()

  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    if (sExclusionFile.contains(file.getFileName.toString)) {
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

    import com.roundeights.hasher.Implicits._
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
      id = (deviceUid + fParent + fileName).sha256.toUpperCase,
      storeName = deviceUid,
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

  override def preVisitDirectory(path: Path, attrs: BasicFileAttributes): FileVisitResult = {
    if (path.getFileName.toString.contains("_files")) {
      FileVisitResult.SKIP_SUBTREE
    }
    else if (sExclusionDir.contains(path.getFileName.toString)) {
      lstExclusionDir.add(path.getFileName.toString)
      lstExclusionFullPath.add(path.getParent.toString + OsConf.fsSeparator + path.getFileName)
      FileVisitResult.SKIP_SUBTREE
    } else {
      FileVisitResult.CONTINUE
    }
  }

  def done(): ArrayBuffer[FileCardSt] = {
    fileCardSt_lst
  }
}

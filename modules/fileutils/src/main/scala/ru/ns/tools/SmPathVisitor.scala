package ru.ns.tools

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import ru.ns.model.{OsConf, SmPath}

import scala.collection.mutable.ArrayBuffer


/**
  *
  * @param glob          glob
  * @param mountPoint    mount point
  * @param sExclusionDir list of ExclusionDir
  */
class SmPathVisitor(glob: String,
                    mountPoint: String, sExclusionDir: util.List[String])
  extends SimpleFileVisitor[Path] {

  private val logger: Logger = Logger(LoggerFactory.getLogger(this.getClass))

  val fileCardSt_lst: ArrayBuffer[SmPath] = ArrayBuffer[SmPath]()
  val pathMatcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:" + glob)
  val lstExclusionDir: util.TreeSet[String] = new util.TreeSet[String]
  val lstExclusionFullPath: util.TreeSet[String] = new util.TreeSet[String]
  var hDevise: Map[String, String] = Map[String, String]()

  override def preVisitDirectory(path: Path, attrs: BasicFileAttributes): FileVisitResult = {

    if (path.getFileName.toString.contains("_files")) {
      FileVisitResult.SKIP_SUBTREE
    }
    else if (sExclusionDir.contains(path.getFileName.toString)) {
      lstExclusionDir.add(path.getFileName.toString)
      lstExclusionFullPath.add(path.getParent.toString + OsConf.fsSeparator + path.getFileName)
      FileVisitResult.SKIP_SUBTREE
    } else {

      // TODO deduplicate here and in visitFile
      val fParent: String =
        if (mountPoint == OsConf.fsSeparator) {
          path.toUri.getPath.drop(1)
        } else {
          if (OsConf.isWindows) {
            path.toUri.getPath.substring(mountPoint.length).drop(2)
          } else {
            path.toUri.getPath.substring(mountPoint.length).drop(1)
          }
        }

      // TODO del "/" in begins like in SmMathingVisitor

      val smPath: SmPath = SmPath(fParent)
      fileCardSt_lst += smPath

      FileVisitResult.CONTINUE
    }
  }

  def done(): ArrayBuffer[SmPath] = {
    fileCardSt_lst
  }
}

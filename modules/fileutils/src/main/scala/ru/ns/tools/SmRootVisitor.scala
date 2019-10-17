package ru.ns.tools

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, _}

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import ru.ns.model.{OsConf, SmPath}

import scala.collection.mutable.ArrayBuffer


/**
  *
  * @param glob       glob
  * @param mountPoint mount point
  */
class SmRootVisitor(glob: String,
                    mountPoint: String)
  extends SimpleFileVisitor[Path] {

  private val logger: Logger = Logger(LoggerFactory.getLogger(this.getClass))

  val pathsList: ArrayBuffer[SmPath] = ArrayBuffer[SmPath]()
  val pathDeniedList: ArrayBuffer[SmPath] = ArrayBuffer[SmPath]()

  override def preVisitDirectory(path: Path, attrs: BasicFileAttributes): FileVisitResult = {

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

    val smPath: SmPath = SmPath(fParent.dropRight(1))

    pathsList += smPath

    FileVisitResult.CONTINUE
  }

  @throws[IOException]
  override def visitFileFailed(path: Path, exc: IOException): FileVisitResult = {
    if (exc.isInstanceOf[java.nio.file.AccessDeniedException]) {
      logger.warn(s"visitFileFailed: ${exc.toString}")

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

      val smPath: SmPath = SmPath(fParent.dropRight(1))

      pathDeniedList += smPath

      return FileVisitResult.SKIP_SUBTREE
    }
    super.visitFileFailed(path, exc)
  }

  def done(): (ArrayBuffer[SmPath], ArrayBuffer[SmPath]) = {
    (pathsList.drop(1), pathDeniedList)
  }
}

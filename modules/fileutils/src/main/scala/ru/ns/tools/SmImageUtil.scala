package ru.ns.tools

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.{File, FileNotFoundException, IOException}
import java.nio.charset.StandardCharsets

import com.google.common.hash.Hashing
import com.typesafe.scalalogging.Logger
import javax.imageio.ImageIO
import org.slf4j.LoggerFactory
import ru.ns.model.OsConf

import scala.concurrent.Future

object SmImageUtil {
  private val logger: Logger = Logger(LoggerFactory.getLogger(this.getClass))
  private val cacheNameDir = "images-cache"

  def getGroupDirName(sha256: String): String = {
    sha256.take(2)
  }

  def getImageKey(sha256: String, fileName: String, extension: String): String = {
    val hashFileName = Hashing.sha256().hashString(fileName, StandardCharsets.UTF_8).toString.toUpperCase
    val fileExtension = if (extension.nonEmpty) s".$extension" else ""

    s"${sha256}_$hashFileName$fileExtension"
  }

  def getImageFullKey(sha256: String, imageKey: String): String = {
    s"$cacheNameDir${OsConf.fsSeparator}${getGroupDirName(sha256)}${OsConf.fsSeparator}$imageKey"
  }

  def saveImageResize(inPathCache: String, origImage: String, fileName: String, extension: String, sha256: String): Future[Option[String]] = {
    var res: Option[String] = None
    val pathCacheName = inPathCache + OsConf.fsSeparator
    try {
      val cacheDir = better.files.File(pathCacheName + cacheNameDir)

      if (cacheDir.exists() || cacheDir.createDirectories().exists) {
        val groupDirName = pathCacheName + cacheNameDir + OsConf.fsSeparator + getGroupDirName(sha256)
        val groupDir = better.files.File(groupDirName)

        val imageKey = getImageKey(sha256, fileName, extension)
        val imageFullKey = getImageFullKey(sha256, imageKey)
        val resizedImageFName = new File(pathCacheName + imageFullKey)

        groupDir.exists || groupDir.createDirectories().exists
        if (!resizedImageFName.exists()) {
          // Target size
          val width = 200
          val height = 200


          val originalImage: BufferedImage = ImageIO.read(new File(origImage)) // Load image from disk
          val resized = originalImage.getScaledInstance(width, height, Image.SCALE_DEFAULT) // Resize

          val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB) // Saving Image back to disk
          bufferedImage.getGraphics.drawImage(resized, 0, 0, null)

          ImageIO.write(bufferedImage, "JPEG", resizedImageFName)

          debug(resizedImageFName)
        }
        res = Some(imageKey)
      }
      else {
        logger.warn(s"Can`t create path = $fileName")
      }
    } catch {
      case e: FileNotFoundException => logger.error("saveImageResize FileNotFoundException", e)
      case e: IOException => logger.error("saveImageResize IOException", e)
    }
    Future.successful(res)
  }
}

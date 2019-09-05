package ru.ns.tools

import java.io.{File, IOException}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.drew.imaging.jpeg.{JpegMetadataReader, JpegProcessingException}
import com.drew.metadata.exif.ExifReader
import com.drew.metadata.iptc.IptcReader
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import ru.ns.model.SmExif

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object SmExifUtil {
  private val logger: Logger = Logger(LoggerFactory.getLogger(this.getClass))

  def debugParam(implicit line: sourcecode.Line, enclosing: sourcecode.Enclosing, args: sourcecode.Args): Unit = {
    logger.debug(s"debugParam ${enclosing.value} : ${line.value}  - "
      + args.value.map(_.map(a => a.source + s"=[${a.value}]").mkString("(", "\n, ", ")")).mkString("")
    )
  }

  def debug[V](value: sourcecode.Text[V])(implicit fullName: sourcecode.FullName): Unit = {
    logger.debug(s"${fullName.value} = ${value.source} : [${value.value}]")
  }

  def getExif(fileName: String): ArrayBuffer[String] = {
    val qwe: ArrayBuffer[String] = ArrayBuffer[String]()
    debug(fileName)

    val file = new File(fileName)
    val hTags = new mutable.HashMap[String, String]()

    if (file.exists()) {
      try {
        val readers = java.util.Arrays.asList(new ExifReader(), new IptcReader()) // We are only interested in handling
        val metadata = JpegMetadataReader.readMetadata(file, readers)
        metadata.getDirectories.forEach { dir =>
          //          logger.info(s"dir - ${dir.toString} \n------------")
          dir.getTags.iterator().forEachRemaining { tag =>
            tag.getTagName match {
              case "Make" | "Model" | "Software"
                   | "Date/Time" | "Date/Time Original" | "Date/Time Digitized"
                   | "Exif Image Width" | "Exif Image Height"
              => hTags += (tag.getTagName -> tag.getDescription)
              case _ => None
            }
            //            logger.info(tag.getTagName + " @ " + hTags.getOrElse(tag.getTagName, "").toString)
            //            logger.info(tag.getTagName + " @ " + tag.getDescription)
          }
        }
        hTags foreach { case (key, value) => logger.info(key + "-->" + value) }
      } catch {
        case e: JpegProcessingException => logger.error("JpegProcessingException", e)
        case e: IOException => logger.error("IOException", e)
      }
    }
    val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    val asd = SmExif(
      Some(LocalDateTime.parse(hTags.getOrElse("Date/Time", ""), formatter)),
      Some(LocalDateTime.parse(hTags.getOrElse("Date/Time Original", ""), formatter)),
      Some(LocalDateTime.parse(hTags.getOrElse("Date/Time Digitized", ""), formatter)),
      hTags.get("Make"), hTags.get("Model"), hTags.get("Software"),
      hTags.get("Exif Image Width"), hTags.get("Exif Image Height")
    )
    logger.info("111")
    logger.info(asd.toString)
    qwe
  }
}

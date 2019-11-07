package ru.ns.tools

import java.io.{File, IOException}
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.drew.imaging.jpeg.{JpegMetadataReader, JpegProcessingException}
import com.drew.imaging.{ImageMetadataReader, ImageProcessingException}
import com.drew.lang.GeoLocation
import com.drew.metadata.exif.{ExifReader, GpsDirectory}
import com.drew.metadata.iptc.IptcReader
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import ru.ns.model.SmExif

object SmExifUtil {
  private val logger: Logger = Logger(LoggerFactory.getLogger(this.getClass))
  private val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
  private val tags = List[String]("Make", "Model", "Software", "Date/Time", "Date/Time Original", "Date/Time Digitized", "Exif Image Width", "Exif Image Height")

  def debugParam(implicit line: sourcecode.Line, enclosing: sourcecode.Enclosing, args: sourcecode.Args): Unit = {
    logger.debug(s"debugParam ${enclosing.value} : ${line.value}  - "
      + args.value.map(_.map(a => a.source + s"=[${a.value}]").mkString("(", "\n, ", ")")).mkString("")
    )
  }

  def debug[V](value: sourcecode.Text[V])(implicit fullName: sourcecode.FullName): Unit = {
    logger.debug(s"${fullName.value} = ${value.source} : [${value.value}]")
  }

  /**
    * https://github.com/drewnoakes/metadata-extractor/blob/master/Source/com/drew/metadata/exif/ExifDirectoryBase.java
    *
    * @param fileName full path File
    * @return
    */
  def getExifByFileName(fileName: String): Option[SmExif] = {
    debug(fileName)

    val file = new File(fileName)
    var hTags = Map.empty[String, String]
    var hGpsTags = Map.empty[String, BigDecimal]

    if (file.exists() && Files.probeContentType(file.toPath) == "image/jpeg") {
      try {
        val readers = java.util.Arrays.asList(new ExifReader(), new IptcReader()) // We are only interested in handling
        val metadata = JpegMetadataReader.readMetadata(file, readers)
        metadata.getDirectories.forEach { dir =>
          dir.getTags.iterator().forEachRemaining { tag =>
            if (tags.contains(tag.getTagName)) {
              hTags = hTags + (tag.getTagName -> tag.getDescription)
            }
            else if (tag.getDirectoryName == "GPS") {
              hTags = hTags + (tag.getTagName -> tag.getDescription)
            }
          }
        }
        metadata.getDirectoriesOfType(classOf[GpsDirectory]).forEach { dir =>
          val lGeoLocation: GeoLocation = dir.getGeoLocation
          if (lGeoLocation != null && !lGeoLocation.isZero) {
            hGpsTags = hGpsTags + ("gpsLatitudeD" -> lGeoLocation.getLatitude)
            hGpsTags = hGpsTags + ("gpsLongitudeD" -> lGeoLocation.getLongitude)
          }
        }
        //        hTags foreach { case (key, value) => logger.info(key + "-->" + value) }
      } catch {
        case e: JpegProcessingException => logger.error("JpegProcessingException", e)
        case e: IOException => logger.error("IOException", e)
      }
    }
    try {
      extractSmExif(hTags, hGpsTags)
    } catch {
      case e: java.time.format.DateTimeParseException =>
        logger.error("DateTimeParseException {}", fileName, e)
        None
      case e: Throwable => logger.error("IOException", e)
        throw e
    }
  }

  def extractSmExif(hTags: Map[String, String], hGpsTags: Map[String, BigDecimal]): Option[SmExif] = {
    Some(SmExif(
      extractDateTimeKeyFromExifMap(hTags, "Date/Time"),
      extractDateTimeKeyFromExifMap(hTags, "Date/Time Original"),
      extractDateTimeKeyFromExifMap(hTags, "Date/Time Digitized"),
      hTags.get("Make"),
      hTags.get("Model"),
      hTags.get("Software"),
      hTags.get("Exif Image Width"),
      hTags.get("Exif Image Height"),
      hTags.get("GPS Version ID"),
      hTags.get("GPS Latitude Ref"),
      hTags.get("GPS Latitude"),
      hTags.get("GPS Longitude Ref"),
      hTags.get("GPS Longitude"),
      hTags.get("GPS Altitude Ref"),
      hTags.get("GPS Altitude"),
      hTags.get("GPS Time-Stamp"),
      hTags.get("GPS Processing Method"),
      hTags.get("GPS Date Stamp"),
      hGpsTags.get("gpsLatitudeD"),
      hGpsTags.get("gpsLongitudeD"),
    )
    )
  }

  def extractDateTimeKeyFromExifMap(hTags: Map[String, String], key: String): Option[java.time.LocalDateTime] = {
    if (hTags.get(key).isDefined && !hTags.getOrElse(key, "").startsWith("0000:00:00")) {
      Some(LocalDateTime.parse(hTags.getOrElse(key, ""), formatter))
    } else {
      None
    }
  }

  def printAllExifByFileName(fileName: String): Unit = {
    debug(fileName)

    val file = new File(fileName)

    if (file.exists()) {
      try {
        val metadata = ImageMetadataReader.readMetadata(file)
        metadata.getDirectories.forEach { dir =>
          logger.info(s"dir - ${dir.toString}\n------------")
          dir.getTags.iterator().forEachRemaining { tag =>
            logger.info(tag.getTagName + " @ " + tag.getDescription)
          }
        }
      } catch {
        case e: ImageProcessingException => logger.error("ImageProcessingException", e)
        case e: IOException => logger.error("IOException", e)
      }
    }
  }
}

package ru.ns.tools

import java.io.{File, IOException}
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.chrono.IsoChronology
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, ResolverStyle, SignStyle}
import java.time.temporal.ChronoField

import com.drew.imaging.jpeg.{JpegMetadataReader, JpegProcessingException}
import com.drew.imaging.mp4.Mp4MetadataReader
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
  private val formatter2 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
  private val tagsJpeg = List[String]("Make", "Model", "Software", "Date/Time", "Date/Time Original", "Date/Time Digitized", "Exif Image Width", "Exif Image Height")
  private val tagsMp4 = List[String]("Creation Time", "Modification Time")

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
  def getExifByFileName(fileName: String, fMimeTypeJava: String): Option[SmExif] = {
    debug(fileName)

    val res = if (fMimeTypeJava == "image/jpeg") {
      fillJpeg(fileName)
    }
    else if (fMimeTypeJava == "video/mp4") {
      fillMp4(fileName)
    } else {
      None
    }
    //    debug(res)

    res
  }

  def fillMp4(fileName: String): Option[SmExif] = {
    val file = new File(fileName)
    var hTags = Map.empty[String, String]
    if (file.exists()) {
      try {
        val metadata = Mp4MetadataReader.readMetadata(file)

        metadata.getDirectories.forEach { dir =>
          dir.getTags.iterator().forEachRemaining { tag =>
            if (tagsMp4.contains(tag.getTagName)) {
              hTags = hTags + (tag.getTagName -> tag.getDescription)
            }
          }
        }
        //        hTags foreach { case (key, value) => logger.info(key + "-->" + value) }
      } catch {
        case e: ImageProcessingException => logger.error("ImageProcessingException", e)
        case e: IOException => logger.error("IOException", e)
      }
    }
    try {
      createMp4SmExif(hTags)
    } catch {
      case e: java.time.format.DateTimeParseException =>
        logger.error("DateTimeParseException {}", fileName, e)
        None
      case e: Throwable => logger.error("IOException", e)
        throw e
    }
  }

  def fillJpeg(fileName: String): Option[SmExif] = {
    val file = new File(fileName)
    var hTags = Map.empty[String, String]
    var hGpsTags = Map.empty[String, BigDecimal]

    if (file.exists() && Files.probeContentType(file.toPath) == "image/jpeg") {
      try {
        val readers = java.util.Arrays.asList(new ExifReader(), new IptcReader()) // We are only interested in handling
        val metadata = JpegMetadataReader.readMetadata(file, readers)
        metadata.getDirectories.forEach { dir =>
          dir.getTags.iterator().forEachRemaining { tag =>
            if (tagsJpeg.contains(tag.getTagName)) {
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
      createJpegSmExif(hTags, hGpsTags)
    } catch {
      case e: java.time.format.DateTimeParseException =>
        logger.error("DateTimeParseException {}", fileName, e)
        None
      case e: Throwable => logger.error("IOException", e)
        throw e
    }
  }

  def createJpegSmExif(hTags: Map[String, String], hGpsTags: Map[String, BigDecimal]): Option[SmExif] = {
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
      hGpsTags.get("gpsLongitudeD")
    )
    )
  }

  def createMp4SmExif(hTags: Map[String, String]): Option[SmExif] = {
    Some(SmExif(
      extractDateTimeKeyMp4FromExifMap(hTags, "Creation Time"),
      extractDateTimeKeyMp4FromExifMap(hTags, "Modification Time")
    )
    )
  }

  def extractDateTimeKeyFromExifMap(hTags: Map[String, String], key: String): Option[java.time.LocalDateTime] = {
    val keyVal = hTags.getOrElse(key, "")

    if (keyVal.nonEmpty) {
      if (keyVal.contains("/")) {
        Some(LocalDateTime.parse(keyVal, formatter2))
      }
      else if (keyVal.contains(":") && !keyVal.startsWith("0000:00:00")) {
        // TODO parse val '2010:05:19 11:33: 7'
        Some(LocalDateTime.parse(keyVal, formatter))
      } else {
        None
      }
    } else {
      None
    }
  }

  /**
    * https://stackoverflow.com/questions/45829799/java-time-format-datetimeformatter-rfc-1123-date-time-fails-to-parse-time-zone-n
    * Sat Jul 18 10:43:20 MSK 2020
    *
    * @param hTags hTags
    * @param key   key
    * @return
    */
  def extractDateTimeKeyMp4FromExifMap(hTags: Map[String, String], key: String): Option[java.time.LocalDateTime] = {
    import scala.jdk.CollectionConverters._
    // custom map for days of week// custom map for days of week
    val dow: scala.collection.Map[java.lang.Long, String] = Map(Long.box(1L) -> "Mon", Long.box(2L) -> "Tue", Long.box(3L) -> "Wed", Long.box(4L) -> "Thu", Long.box(5L) -> "Fri", Long.box(6L) -> "Sat", Long.box(7L) -> "Sun")
    // custom map for months
    val moy: scala.collection.Map[java.lang.Long, String] = Map(Long.box(1L) -> "Jan", Long.box(2L) -> "Feb", Long.box(3L) -> "Mar", Long.box(4L) -> "Apr", Long.box(5L) -> "May", Long.box(6L) -> "Jun", Long.box(7L) -> "Jul", Long.box(8L) -> "Aug", Long.box(9L) -> "Sep", Long.box(10L) -> "Oct", Long.box(11L) -> "Nov", Long.box(12L) -> "Dec")

    val fmt = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .parseLenient()
      .optionalStart()
      .appendText(ChronoField.DAY_OF_WEEK, dow.asJava)
      .appendLiteral(' ')
      .appendText(ChronoField.MONTH_OF_YEAR, moy.asJava)
      .appendLiteral(' ')
      .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
      .appendLiteral(' ')

      .appendValue(ChronoField.HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
      .appendLiteral(' ')

      .appendOffset("+HH:MM", "GMT")
      .appendLiteral(' ')

      .appendValue(ChronoField.YEAR, 4) // 2 digit year not handled
      .optionalEnd()

      // use the same resolver style and chronology
      .toFormatter().withResolverStyle(ResolverStyle.SMART).withChronology(IsoChronology.INSTANCE)

    if (hTags.contains(key)) {
      Some(LocalDateTime.parse(hTags.getOrElse(key, ""), fmt))
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

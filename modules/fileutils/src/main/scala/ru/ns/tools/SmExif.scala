package ru.ns.tools

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object SmExif {
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
    //    import scala.collection.JavaConverters._
    //    import com.drew.imaging.ImageMetadataReader
    //    import com.drew.metadata.Metadata
    /*
       import com.drew.imaging.ImageProcessingException
       import com.drew.imaging.jpeg.JpegMetadataReader
       import com.drew.imaging.jpeg.JpegProcessingException
       import com.drew.imaging.jpeg.JpegSegmentMetadataReader
       import com.drew.metadata.exif.ExifReader
       import com.drew.metadata.iptc.IptcReader
       import java.io.IOException
       import java.util
   */
    import com.drew.imaging.ImageMetadataReader
    import java.io.File

    val file = new File("/home/user/" + fileName)

    if (file.exists()) {
      val metadata = ImageMetadataReader.readMetadata(file)

      println(metadata)
      println(metadata.getDirectories)

      metadata.getDirectories.forEach { dir =>
        println(dir)
        dir.getTags.iterator().forEachRemaining { tag =>
          println(tag)
        }
      }
    }
    qwe
  }
}

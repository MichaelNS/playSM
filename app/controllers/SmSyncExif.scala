package controllers

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.Logger
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.{OsConf, SmExif, SmExifGoo}
import ru.ns.tools.{FileUtils, SmExifUtil}
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
  * Created by ns on 12.03.2018
  */
@Singleton
class SmSyncExif @Inject()(val database: DBService)
  extends InjectedController {

  val logger: Logger = play.api.Logger(getClass)


  def calcExif(deviceUid: String): Action[AnyContent] = Action.async {

    database.runAsync(
      (for {
        (fcRow, exifRow) <- Tables.SmFileCard joinLeft Tables.SmExif on ((fc, exif) => {
          fc.id === exif.id
        }) if fcRow.deviceUid === deviceUid && (fcRow.fMimeTypeJava === "image/jpeg" || fcRow.fMimeTypeJava === "video/mp4") && exifRow.isEmpty}
        yield (fcRow.id, fcRow.fParent, fcRow.fName, fcRow.fMimeTypeJava)
        ).result)
      .map { rowSeq =>
        FileUtils.getDeviceInfo(deviceUid) onComplete {
          case Success(device) =>
            if (device.isDefined) {
              val mountPoint = device.head.mountpoint
              rowSeq.foreach { cFc => writeExif(cFc._1, mountPoint + OsConf.fsSeparator + cFc._2 + cFc._3, cFc._4.getOrElse("")) }

              database.runAsync((for {uRow <- Tables.SmDevice if uRow.uid === deviceUid} yield uRow.exifDate)
                .update(Some(LocalDateTime.now())))
                .map(_ => logger.info(s"Exif complete for device $deviceUid"))
            }
          case Failure(ex)
          => logger.error(s"calcCRC error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
        }
        Ok("run calcExif")
      }
  }

  def writeExif(id: String, fileName: String, fMimeTypeJava: String): Future[Int] = {
    debugParam

    val smExif: Option[SmExif] = SmExifUtil.getExifByFileName(fileName, fMimeTypeJava)
    if (smExif.isDefined) {
      val cRow = Tables.SmExifRow(id,
        smExif.get.dateTime,
        smExif.get.dateTimeOriginal,
        smExif.get.dateTimeDigitized,
        smExif.get.make,
        smExif.get.model,
        smExif.get.software,
        smExif.get.exifImageWidth,
        smExif.get.exifImageHeight,
        smExif.get.gpsVersionID,
        smExif.get.gpsLatitudeRef,
        smExif.get.gpsLatitude,
        smExif.get.gpsLongitudeRef,
        smExif.get.gpsLongitude,
        smExif.get.gpsAltitudeRef,
        smExif.get.gpsAltitude,
        smExif.get.gpsTimeStamp,
        smExif.get.gpsProcessingMethod,
        smExif.get.gpsDateStamp,
        smExif.get.gpsLatitudeD,
        smExif.get.gpsLongitudeD
      )
      val insRes = database.runAsync(Tables.SmExif.insertOrUpdate(models.SmExif.apply(cRow).data.toRow))
      insRes
    }
    else {
      Future.successful(0)
    }
  }

  def getExif(fileName: String): Action[AnyContent] = Action {
    debugParam

    SmExifUtil.getExifByFileName("/tmp/123/" + fileName, java.nio.file.Files.probeContentType(new java.io.File(fileName).toPath))
    SmExifUtil.printAllExifByFileName("/tmp/123/" + fileName)

    Ok("Job run")
  }

  def viewAllGps: Action[AnyContent] = Action.async {
    val lstSmExifGoo = ArrayBuffer[SmExifGoo]()

    database.runAsync(
      (for {
        (fcRow, exifRow) <- Tables.SmFileCard join Tables.SmExif on ((fc, exif) => {
          fc.id === exif.id && exif.gpsLatitude.nonEmpty
        })}
        yield (fcRow.id, fcRow.fParent, fcRow.fName, exifRow.gpsLatitudeDec, exifRow.gpsLongitudeDec)
        ).result)
      .map { rowSeq =>
        rowSeq.foreach { cExif =>
          lstSmExifGoo += SmExifGoo(cExif._2 + cExif._3, new com.drew.lang.GeoLocation(cExif._4.get.toDouble, cExif._5.get.toDouble))
        }
        Ok(views.html.gps(lstSmExifGoo)())
      }
  }
}

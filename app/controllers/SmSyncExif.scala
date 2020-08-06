package controllers

import java.nio.file.attribute.FileTime
import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import models.db.Tables
import org.joda.time.DateTime
import play.api.mvc._
import play.api.{Configuration, Logger}
import ru.ns.model.{OsConf, SmExif, SmExifGoo}
import ru.ns.tools.{FileUtils, SmExifUtil}
import services.db.DBService
import slick.jdbc.GetResult
import slick.sql.SqlStreamingAction
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by ns on 12.03.2018
  */
@Singleton
class SmSyncExif @Inject()(cc: MessagesControllerComponents, config: Configuration, val database: DBService)
  extends MessagesAbstractController(cc) {

  val logger: Logger = play.api.Logger(getClass)

  val smSyncDeviceStream = new SmSyncDeviceStream(cc, config, database)

  implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

  case class FolderSync(deviceUid: String, mountPoint: String, folderName: String)

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

  def getDirsDiffModifyDate: Action[AnyContent] = Action.async {

    val qry = sql"""
       SELECT F_PARENT
       FROM sm_file_card
          INNER JOIN sm_exif ON sm_exif.ID = sm_file_card.ID
       WHERE sm_exif.ID IS NOT NULL
       AND   sm_exif.DATE_TIME IS NOT NULL
       AND   (DATE_PART('day',sm_exif.DATE_TIME) != DATE_PART('day',sm_file_card.F_LAST_MODIFIED_DATE)
           OR DATE_PART('hour',sm_exif.DATE_TIME) != DATE_PART('hour',sm_file_card.F_LAST_MODIFIED_DATE)
             )
       GROUP BY F_PARENT
       ORDER BY F_PARENT
      """
      .as[String]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.exif.diff_modify_dates_dirs(rowSeq)())
    }

  }

  def getFilesDiffModifyDateByParent(fParent: String): SqlStreamingAction[Vector[(String, String, String, DateTime, DateTime, String, String, String)], (String, String, String, DateTime, DateTime, String, String, String), Effect] = {

    sql"""
       SELECT F_PARENT,
              sm_file_card.F_NAME,
              sm_file_card.sha256,
              TO_CHAR(sm_file_card.F_LAST_MODIFIED_DATE,'YYYY-MM-DD HH24:MI:SS') F_LAST_MODIFIED_DATE,
              TO_CHAR(sm_exif.DATE_TIME,'YYYY-MM-DD HH24:MI:SS') EXIF_DATE_TIME,
              sm_exif.MAKE,
              sm_exif.MODEL,
              sm_file_card.device_uid
       FROM sm_file_card
          INNER JOIN sm_exif ON sm_exif.ID = sm_file_card.ID
       WHERE sm_exif.ID IS NOT NULL
       AND   sm_exif.DATE_TIME IS NOT NULL
       AND   (DATE_PART('day',sm_exif.DATE_TIME) != DATE_PART('day',sm_file_card.F_LAST_MODIFIED_DATE)
           OR DATE_PART('hour',sm_exif.DATE_TIME) != DATE_PART('hour',sm_file_card.F_LAST_MODIFIED_DATE)
             )
       AND   sm_file_card.f_parent = '#$fParent'
       ORDER BY DATE_TIME DESC
      """
      .as[(String, String, String, DateTime, DateTime, String, String, String)]
  }

  def vFilesDiffModifyDateByParent(fParent: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>

    database.runAsync(getFilesDiffModifyDateByParent(fParent)).map { rowSeq =>
      Ok(views.html.exif.diff_modify_dates_files(rowSeq, fParent))
    }
  }

  /**
    * https://stackoverflow.com/questions/9198184/setting-file-creation-timestamp-in-java
    *
    * @param fParent fParent
    * @return
    */
  def setDiffModifyDateFilesByParent(fParent: String): Action[AnyContent] = Action.async {
    database.runAsync(getFilesDiffModifyDateByParent(fParent)).map { rowSeq =>
      FileUtils.getDevicesInfo() onComplete {
        case Success(lstDevices) =>
          val devices = lstDevices.map(t => t.uuid -> t.mountpoint).toMap
          val folders2Sync = scala.collection.mutable.Set[FolderSync]()

          rowSeq.foreach { row =>
            if (devices.contains(row._8)) {
              // TODO нужна функция, которая возвращает точку монтирования и полный путь к файлу
              val mountPoint = devices.getOrElse(row._8, "")
              val fileName = s"$mountPoint/${row._1}${row._2}"
              logger.info(fileName)

              val file = better.files.File(fileName)
              //              file.update("creationTime", FileTime.fromMillis(row._5.getMillis))

//              file.update("lastModifiedTime", FileTime.fromMillis(row._5.getMillis))
              val attributes = file.attributes
              if (attributes.creationTime().toMillis > attributes.lastModifiedTime().toMillis) {
                file.update("creationTime", FileTime.fromMillis(row._5.getMillis))
              }

              val folderSync = FolderSync(row._8, mountPoint, row._1)
              if (!folders2Sync.contains(folderSync)) {
                folders2Sync += folderSync
              }
            }
          }

          folders2Sync.foreach { folder =>
            FileUtils.getDeviceInfo(folder.deviceUid).map { device =>
              if (device.isDefined) {
                // TODO rewrite to play framework style
                smSyncDeviceStream.syncPath(folder.folderName, folder.deviceUid, device.get.mountpoint)
              }
            }
            //            smSyncDeviceStream.syncSingleNamePath(folder.folderName, folder.deviceUid)
            //                smSyncDeviceStream.syncPath(folder.folderName, folder.deviceUid, device.get.mountpoint)
            //            routes.SmSyncDeviceStream.syncSingleNamePath(folder.folderName, folder.mountPoint)
          }
        case Failure(exception) => logger.error(s"setDiffModifyDateFilesByParent get devices error: ${exception.toString}\nStackTrace:\n ${exception.getStackTrace.mkString("\n")}")
      }
    }

    Future.successful(Ok("Done"))
  }
}

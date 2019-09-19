package controllers

import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.{OsConf, SmExif}
import ru.ns.tools.{FileUtils, SmExifUtil}
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
  * Created by ns on 12.03.2018
  */
@Singleton
class SmSyncExif @Inject()(val database: DBService)
  extends InjectedController {

  val logger = play.api.Logger(getClass)


  def calcExif(device: String): Action[AnyContent] = Action.async {

    database.runAsync(
      (for {
        (fcRow, exifRow) <- Tables.SmFileCard joinLeft Tables.SmExif on ((fc, exif) => {
          fc.id === exif.id
        }) if fcRow.storeName === device && fcRow.fMimeTypeJava === "image/jpeg" && exifRow.isEmpty}
        yield (fcRow.id, fcRow.fParent, fcRow.fName)
        ).to[List].result)
      .map { rowSeq =>
        FileUtils.getDevicesInfo() onComplete {
          case Success(sucLabel2Drive) =>
            val mountPoint = sucLabel2Drive.filter(_.uuid == device).head.mountpoint
            rowSeq.foreach { cFc => writeExif(cFc._1, mountPoint + OsConf.fsSeparator + cFc._2 + cFc._3) }
          case Failure(ex)
          => logger.error(s"calcCRC error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
        }
        Ok("run calcExif")
      }
  }

  def writeExif(id: String, fileName: String): Future[Int] = {
    debugParam

    val smExif: Option[SmExif] = SmExifUtil.getExifByFileName(fileName)
    if (smExif.isDefined) {
      val cRow = Tables.SmExifRow(id, smExif.get.dateTime, smExif.get.dateTimeOriginal, smExif.get.dateTimeDigitized, smExif.get.make, smExif.get.model, smExif.get.software, smExif.get.exifImageWidth, smExif.get.exifImageHeight)
      val insRes = database.runAsync(Tables.SmExif.insertOrUpdate(models.SmExif.apply(cRow).data.toRow))
      insRes
    }
    else {
      Future.successful(0)
    }
  }

  def getExif(fileName: String): Action[AnyContent] = Action {
    debugParam

    SmExifUtil.getExifByFileName("c:/tmp/images/" + fileName)
    SmExifUtil.printAllExifByFileName("c:/tmp/images/" + fileName)

    Ok("Job run")
  }
}

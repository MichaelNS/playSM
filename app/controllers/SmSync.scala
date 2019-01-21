package controllers

import java.time.LocalDateTime
import java.util

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.db.Tables
import models.{DeviceView, SmDevice, SmFileCard}
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.OsConf
import ru.ns.tools.FileUtils
import services.db.DBService
import slick.jdbc.GetResult
import utils.db.SmPostgresDriver.api._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by ns on 23.02.2017.
  */
@Singleton
class SmSync @Inject()(val database: DBService)
  extends InjectedController {

  def refreshDevice: Action[AnyContent] = Action.async {
    database.runAsync(Tables.SmDevice.sortBy(_.uid).to[List].map(_.uid).result).map { rowSeq =>
      Logger.debug(pprint.apply(rowSeq).toString())

      FileUtils.getDevicesInfo() onComplete {
        case Success(lstDevices) =>
          lstDevices.foreach { device =>
            debug(device)

            if (rowSeq.contains(device.uuid)) {
              Logger.info(s"Device [${device.toString}] already exists")
            } else {
              val cRow = Tables.SmDeviceRow(-1, device.name, device.label, device.uuid, LocalDateTime.MIN)

              val insRes = database.runAsync((Tables.SmDevice returning Tables.SmDevice.map(_.id)) += SmDevice.apply(cRow).data.toRow)
              insRes onComplete {
                case Success(suc) => Logger.debug(s"add device = $suc")
                case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
              }
            }
          }
        case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
      }

      Redirect(routes.SmSync.deviceImport())
    }
  }

  def deviceImport: Action[AnyContent] = Action.async {
    implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

    val qry = sql"""
      SELECT
        x2."NAME",
        x2."LABEL",
        x2."UID",
        x2."DESCRIBE",
        x2."SYNC_DATE",
        x2."VISIBLE"
      FROM "sm_device" x2
      ORDER BY x2."LABEL"
      """
      .as[(String, String, String, String, DateTime, Boolean)]
    database.runAsync(qry).map { rowSeq =>
      val devices = ArrayBuffer[DeviceView]()
      rowSeq.foreach { p => devices += DeviceView(name = p._1, label = p._2, uid = p._3, describe = p._4, syncDate = p._5, visible = p._6, withOutCrc = 0) }

      Ok(views.html.device_import(devices))
    }
  }

  def syncDevice(deviceUid: String): Action[AnyContent] = Action {
    debugParam

    FileUtils.getDevicesInfo() onComplete {
      case Success(label2Drive) =>
        val curDevice = label2Drive.filter(_.uuid == deviceUid)
        Logger.debug(pprint.apply(curDevice).toString())

        if (label2Drive.nonEmpty && curDevice.nonEmpty) {
          syncDb(deviceUid, curDevice.head.mountpoint)
        } else {
          Logger.warn(s"device [$deviceUid] is not available")
        }
      case Failure(t) => Logger.error(s"syncDevice: = ${t.getMessage}")
    }
    Redirect(routes.SmApplication.deviceIndex(deviceUid))
  }

  def syncDb(deviceUid: String, mountPoint: String): Unit = {
    debugParam

    val config = ConfigFactory.load("scanImport.conf")
    val sExclusionDir = config.getStringList("paths2Scan.exclusionPath")
    val sExclusionFile = config.getStringList("paths2Scan.exclusionFile")

    val impPath: util.List[String] = config.getStringList("paths2Scan.volumes." + deviceUid)
    debug(impPath.size)
    Logger.debug(pprint.apply(impPath.asScala.map(c => mountPoint + OsConf.fsSeparator + c)).toString())

    impPath.asScala.foreach { path =>
      val start = System.currentTimeMillis
      Logger.debug(s"mergeDrive2Db start -> path = [$path]")

      val res = mergeDrive2Db(deviceUid, mountPoint, path, sExclusionDir, sExclusionFile)
      res.onComplete {
        case Success(suc) => Logger.debug(s"mergeDrive2Db complete [${System.currentTimeMillis - suc}]   path = [$path]")
        case Failure(t) => Logger.error(s"mergeDrive2Db  error: = ${t.getMessage}")
      }
      Logger.info(s"mergeDrive2Db done start job -> path = [$path]   Elapsed time: ${System.currentTimeMillis - start} ms")
    }

    // update last device sync
    Logger.info(s"update last device sync = [$deviceUid]")
    val update = {
      val q = for {uRow <- Tables.SmDevice if uRow.uid === deviceUid} yield uRow.syncDate
      q.update(LocalDateTime.now())
    }
    database.runAsync(update).map { _ => Logger.info(s"Updated device $deviceUid") }
    Logger.info(s"Sync device is done = $deviceUid")
  }

  def mergeDrive2Db(deviceUid: String, mountPoint: String, impPath: String, sExclusionDir: util.List[String], sExclusionFile: util.List[String])
  : Future[Long] = Future[Long] {
    val start = System.currentTimeMillis

    val hSmBoFileCard = FileUtils.readDirRecursive(impPath, deviceUid, mountPoint, sExclusionDir, sExclusionFile)
    val rowSeq = database.runAsync(Tables.SmFileCard
      .filter(_.storeName === deviceUid).filter(_.fParent === impPath)
      .unionAll(Tables.SmFileCard.filter(_.storeName === deviceUid).filter(_.fParent startsWith impPath + OsConf.fsSeparator)
      ).map(fld => (fld.id, fld.fLastModifiedDate)).to[List].result).map(rowSeq => rowSeq)
    rowSeq.onComplete {
      case Success(suc) =>
        val hInMap: Map[String, List[(String, LocalDateTime)]] = suc.groupBy(_._1)
        Logger.debug(s"mergeDrive2Db -> path = [$impPath]  hSmBoFileCard.size = [${hSmBoFileCard.size}]  rowSeq.size = [${suc.size}]   " +
          s"Elapsed time: ${System.currentTimeMillis - start} ms")

        var insCnt = 0
        hSmBoFileCard.foreach { value => // add FC
          if (!hInMap.contains(value.id)) {
            insCnt += 1 // TODO fix to bulk INS
            if (insCnt > 900) {
              Logger.warn("sleep ins")
              Thread.sleep(5000)
              insCnt = 0
            }
            val cRow = Tables.SmFileCardRow(value.id, value.storeName, value.fParent, value.fName, value.fExtension,
              value.fCreationDate, value.fLastModifiedDate, value.fSize, value.fMimeTypeJava, None, value.fNameLc)

            val insRes = database.runAsync((Tables.SmFileCard returning Tables.SmFileCard.map(_.id)) += SmFileCard.apply(cRow).data.toRow)
            insRes onComplete {
              case Success(insSuc) => Logger.debug(s"Inserted FC = $insSuc   $cRow")
              case Failure(t) => Logger.error(s"Insert to DB: = ${t.getMessage}")
            }
          } else { // upd FC
            if (hInMap(value.id).head._2 != value.fLastModifiedDate) {
              val update = {
                val q = for {uRow <- Tables.SmFileCard if uRow.id === value.id} yield (uRow.sha256, uRow.fCreationDate, uRow.fLastModifiedDate, uRow.fSize)
                q.update((None, value.fCreationDate, value.fLastModifiedDate, value.fSize))
              }
              database.runAsync(update).map { _ => Logger.debug(s"Updated key ${value.id}") }
            }
          }
        }
        delFromDb(suc.map(p => p._1), hSmBoFileCard.map(p => p.id), impPath)
      case Failure(t) => Logger.error(s"Get from DB: = ${t.getMessage}")
    }
    Logger.info(s"mergeDrive2Db end -> path = [$impPath]   Elapsed time: ${System.currentTimeMillis - start} ms")
    start
  }

  def delFromDb(idDb: Seq[String], idDevice: Seq[String], path: String): Unit = {
    val start = System.currentTimeMillis

    Logger.debug(s"delFromDb - path = [$path]   idDb.size = [${idDb.size}]  idDevice.size = [${idDevice.size}]")
    debug(idDb diff idDevice)

    (idDb diff idDevice).foreach { key =>
      val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === key).delete)
      insRes onComplete {
        case Success(suc) => Logger.debug(s"del [$suc] row , id = [$key]")
        case Failure(t) => Logger.error(s"Delete from DB: = ${t.getMessage}")
      }
    }

    Logger.info(s"delFromDb - is done, path = [$path]   Elapsed time: ${System.currentTimeMillis - start} ms")
  }

  def calcCRC(device: String): Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxCalcFiles = config.getBytes("CRC.maxCalcFiles")
    val maxSizeFiles: Long = config.getBytes("CRC.maxSizeFiles")

    Logger.info(s"calcCRC maxSizeFiles = $maxSizeFiles   maxCalcFiles = $maxCalcFiles")

    database.runAsync(Tables.SmFileCard
      .filter(_.storeName === device)
      .filter(_.sha256.isEmpty)
      .filter(size => size.fSize.>(0L) && size.fSize.<=(maxSizeFiles))
      .sortBy(_.fParent.asc)
      .take(maxCalcFiles)
      .to[List].result).map { rowSeq =>

      FileUtils.getDevicesInfo() onComplete {
        case Success(sucLabel2Drive) =>
          debug(sucLabel2Drive)
          val mountPoint = sucLabel2Drive.filter(_.uuid == device).head.mountpoint

          rowSeq.foreach { row =>
            try {
              val sha = FileUtils.getGuavaSha256(mountPoint + OsConf.fsSeparator + row.fParent + row.fName)
              if (sha != "") {
                val update = {
                  val q = for (uRow <- Tables.SmFileCard if uRow.id === row.id) yield uRow.sha256
                  q.update(Some(sha))
                }
                database.runAsync(update).map(_ => Logger.debug(s"calcCRC Set sha256 for key ${row.id}   path ${row.fParent} ${row.fName}"))
              }
            } catch {
              case _: java.io.FileNotFoundException | _: java.io.IOException => None
            }
          }
          Logger.info(s"calcCRC Done for device = $device")

        case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
      }
      Ok("Job run")
    }
  }
}

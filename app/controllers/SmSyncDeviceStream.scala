package controllers

import java.nio.file.Paths
import java.time.LocalDateTime

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import javax.inject.{Inject, Singleton}
import models.db.Tables
import models.{DeviceView, SmDevice}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.OsConf
import ru.ns.tools.FileUtils
import services.db.DBService
import slick.jdbc.GetResult
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by ns on 12.03.2018
  */
@Singleton
class SmSyncDeviceStream @Inject()(config: Configuration, val database: DBService)
  extends InjectedController {

  val logger = play.api.Logger(getClass)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def refreshDevice: Action[AnyContent] = Action.async {
    database.runAsync(Tables.SmDevice.sortBy(_.uid).map(_.uid).result).map { rowSeq =>
      logger.debug(pprint.apply(rowSeq).toString())

      FileUtils.getDevicesInfo() onComplete {
        case Success(lstDevices) =>
          lstDevices.foreach { device =>
            debug(device)

            if (rowSeq.contains(device.uuid)) {
              logger.info(s"Device [${device.toString}] already exists")
            } else {
              val cRow = Tables.SmDeviceRow(-1, device.name, device.label, device.uuid, LocalDateTime.MIN)

              val insRes = database.runAsync((Tables.SmDevice returning Tables.SmDevice.map(_.id)) += SmDevice.apply(cRow).data.toRow)
              insRes onComplete {
                case Success(suc) => logger.debug(s"add device = $suc")
                case Failure(ex) => logger.error(s"refreshDevice 1 error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
              }
            }
          }
        case Failure(ex) => logger.error(s"refreshDevice 2 error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
      }

      Redirect(routes.SmApplication.smIndex())
    }
  }

  def deviceImport: Action[AnyContent] = Action.async {
    implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

    val qry = sql"""
      SELECT
        x2.name,
        x2.label,
        x2.uid,
        x2.describe,
        x2.sync_date,
        x2.visible,
        x2.reliable
      FROM sm_device x2
      ORDER BY x2.label
      """
      .as[(String, String, String, String, DateTime, Boolean, Boolean)]
    database.runAsync(qry).map { rowSeq =>
      val devices = ArrayBuffer[DeviceView]()
      rowSeq.foreach { p => devices += DeviceView(name = p._1, label = p._2, uid = p._3, describe = p._4, syncDate = p._5, visible = p._6, reliable = p._7, withOutCrc = 0) }

      Ok(views.html.device_import(devices))
    }
  }

  /**
    * Call from [[views.html.device_import]] and [[views.html.smd_index]]
    *
    * used [[SmSyncDeviceStream.syncPath]]
    *
    * @param deviceUid device Uid
    * @return Action.async
    */
  def syncDevice(deviceUid: String): Action[AnyContent] = Action.async {
    debugParam
    FileUtils.getDeviceInfo(deviceUid).map { device =>
      if (device.isDefined) {
        try {
          val impPath = config.get[Seq[String]]("paths2Scan.volumes." + deviceUid)
          debug(s"${impPath.length} : $impPath")
          logger.debug(pprint.apply(impPath.map(c => device.get.mountpoint + OsConf.fsSeparator + c)).toString())
          Source.fromIterator(() => impPath.iterator)
            .throttle(elements = 1, 10.millisecond, maximumBurst = 2, ThrottleMode.shaping)
            .mapAsync(1)(syncPath(_, deviceUid, device.get.mountpoint)
            ).runWith(Sink.ignore).onComplete {
            case Success(res) =>
              logger.info("done syncDevice, pathConfig " + res.toString + " " + impPath)

              database.runAsync((for {uRow <- Tables.SmDevice if uRow.uid === deviceUid} yield uRow.syncDate)
                .update(LocalDateTime.now()))
                .map(_ => logger.info(s"Sync complete for device $deviceUid"))

            case Failure(ex) =>
              logger.error(s"syncDevice error: ${ex.toString}")

              database.runAsync((for {uRow <- Tables.SmDevice if uRow.uid === deviceUid} yield uRow.syncDate)
                .update(LocalDateTime.now()))
                .map(_ => logger.info(s"Sync complete for device $deviceUid"))

          }
          Redirect(routes.SmSyncDeviceStream.deviceImport())
        } catch {
          case ex: com.typesafe.config.ConfigException =>
            logger.error(s"No config in [scanImport.conf] for device $deviceUid", ex)
            NotFound(ex.toString)
        }
      } else {
        Ok("device mountpoint is empty")
      }
    }
  }

  /**
    * sync path
    *
    * see [[SmSyncDeviceStream.syncDevice]]
    * used [[ru.ns.tools.FileUtils.getPathesRecursive]]
    * used [[SmSyncDeviceStream.mergePath2Db]]
    *
    * @param path2scan  paths to scan - (home/user/Documents)
    * @param deviceUid  deviceUid
    * @param mountPoint mountPoint
    * @return Future
    */
  def syncPath(path2scan: String, deviceUid: String, mountPoint: String): Future[Done] = {
    val funcName = "syncPath"
    val start = System.currentTimeMillis


    val resPath = Source.fromIterator(() => FileUtils.getPathesRecursive
    (path2scan.toString, mountPoint, config.get[Seq[String]]("paths2Scan.exclusionPath")).iterator)
      .throttle(elements = 1, per = 10.millisecond, maximumBurst = 10, mode = ThrottleMode.shaping)
      .map { path =>
        mergePath2Db(deviceUid = deviceUid, mountPoint = mountPoint,
          path.fParent,
          config.get[Seq[String]]("paths2Scan.exclusionFile")
        )
      }
      .recover { case t: Throwable =>
        logger.error("Error retrieving output from flowA. Resuming without them.", t)
        None
      }
      .runWith(Sink.ignore)

    resPath.onComplete {
      case Success(res) => logger.info(s"$funcName " + res.toString + " " + path2scan + s" ${System.currentTimeMillis - start} ms")
      case Failure(ex) => logger.error(s"syncDevice error: ${ex.toString}")
    }

    resPath
  }

  def syncSingleNamePath(path2scan: String, deviceUid: String): Action[AnyContent] = Action {
    FileUtils.getDeviceInfo(deviceUid).map { device =>
      if (device.isDefined) {
        syncPath(path2scan, deviceUid, device.get.mountpoint)
      }
    }
    Ok("Job run")
  }

  /**
    *
    * used [[ru.ns.tools.FileUtils.getFilesFromStore]]
    *
    * @param deviceUid      deviceUid
    * @param mountPoint     mountPoint
    * @param impPath        impPath
    * @param sExclusionFile list of ExclusionFile
    * @return
    */
  def mergePath2Db(deviceUid: String,
                   mountPoint: String,
                   impPath: String,
                   sExclusionFile: Seq[String]
                  ): Future[(Long, Long, Future[Int])] = {
    val funcName = "mergePath2Db"
    val start = System.currentTimeMillis
    val lstToIns = ArrayBuffer[Tables.SmFileCard#TableElementType]()
    val hSmBoFileCard = FileUtils.getFilesFromStore(impPath, deviceUid, mountPoint, sExclusionFile)
    val hInMap = database.runAsync(Tables.SmFileCard
      .filter(_.storeName === deviceUid).filter(_.fParent === impPath)
      .map(fld => (fld.id, fld.fLastModifiedDate)).result)
      .map { dbGet =>
        val hInMap: Map[String, List[(String, LocalDateTime)]] = dbGet.groupBy(_._1)
        //        logger.debug(s"$funcName -> path = [$impPath]  hSmBoFileCard.size = [${hSmBoFileCard.size}]  rowSeq.size = [${dbGet.size}]   get 2 lists time: ${System.currentTimeMillis - start} ms")
        hInMap
      }
    val resMerge = hInMap.map { hInMap =>
      hSmBoFileCard.foreach { value => // add FC
        if (!hInMap.contains(value.id)) {
          lstToIns += Tables.SmFileCardRow(value.id, value.storeName, value.fParent, value.fName, value.fExtension,
            value.fCreationDate, value.fLastModifiedDate, value.fSize, value.fMimeTypeJava, None, value.fNameLc)
        } else { // upd FC
          if (hInMap(value.id).head._2 != value.fLastModifiedDate) {
            logger.info("1 UPD " + value.storeName + " " + value.fParent + " " + value.fName)
            database.runAsync(
              (for {uRow <- Tables.SmFileCard if uRow.id === value.id} yield (uRow.sha256, uRow.fCreationDate, uRow.fLastModifiedDate, uRow.fSize))
                .update((None, value.fCreationDate, value.fLastModifiedDate, value.fSize))
            ).map { _ => logger.debug(s"$funcName -> path = [$impPath]   Updated key ${value.id}") }
          }
        }
      }
      delFromDb(hInMap.keys.toSeq, hSmBoFileCard.map(p => p.id).toSeq, impPath)
      if (lstToIns.nonEmpty) logger.info(s"$funcName -> path = [$impPath]   lstToIns.size ${lstToIns.size}")

      (start, System.currentTimeMillis - start, database.runAsync((Tables.SmFileCard returning Tables.SmFileCard.map(_.id)) ++= lstToIns).map(_.size))
    }
    resMerge.onComplete {
      case Success(res) => res._3.onComplete {
        case Success(cntIns) => if (cntIns > 0) logger.info(s"$funcName inserted [$cntIns] rows   path = [$impPath]   " + s"Elapsed time: ${res._2} ms " + s"All time: ${System.currentTimeMillis - res._1} ms ") //cntIns
        case Failure(ex) => logger.error(s"$funcName 1 error: ${ex.toString}}")
        //        case Failure(ex) => logger.error(s"$funcName 1 error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
      }
      case Failure(ex) => logger.error(s"$funcName 2 error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
    }
    resMerge
  }


  // todo change res delFromDb to future
  def delFromDb(idDb: Seq[String], idDevice: Seq[String], path: String): Unit = {
    val start = System.currentTimeMillis

    val delDiff = idDb diff idDevice
    if (delDiff.nonEmpty) {
      debug(delDiff)
    }

    delDiff.foreach { key =>
      val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === key).delete)
      insRes onComplete {
        case Success(suc) => logger.debug(s"deleted [$suc] row , id = [$key]")
        case Failure(ex) => logger.error(s"delFromDb -> Delete from DB error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
      }
    }

    logger.debug(s"delFromDb - is done, path = [$path] "
      + s"idDb.size = [${idDb.size}]  idDevice.size = [${idDevice.size}]   delDiff.size = ${delDiff.size}"
      + s"  Elapsed time: ${System.currentTimeMillis - start} ms")
  }

  def deleteNonExistsFpathInDb(deviceUid: String): Action[AnyContent] = Action {
    debugParam
    FileUtils.getDevicesInfo(deviceUid).map { devices =>
      debug(devices)
      val device = devices.find(_.uuid == deviceUid)
      debug(device)
      if (device.isDefined) {
        database.runAsync(Tables.SmFileCard
          .filter(_.storeName === deviceUid)
          .map(fld => fld.fParent)
          .distinct.result)
          .map { dbGet =>
            dbGet.foreach { cPath =>
              if (!Paths.get(device.get.mountpoint + OsConf.fsSeparator + cPath).toFile.exists) {
                logger.debug(s"deleteNonExistsFpathInDb ${device.get.mountpoint + OsConf.fsSeparator + cPath}")
                val insRes = database.runAsync(Tables.SmFileCard.filter(_.storeName === deviceUid).filter(_.fParent === cPath).delete)
                insRes onComplete {
                  case Success(suc) => logger.debug(s"deleted [$suc] row , fParent = [$cPath]")
                  case Failure(ex) => logger.error(s"delFromDb -> Delete from DB error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
                }
              }
            }
          }
        logger.info("DONE deleteNonExistsFpathInDb")
      }
    }
    Ok("Job run")
  }

  def calcCRC(device: String): Action[AnyContent] = Action.async {
    val maxCalcFiles: Long = config.get[Long]("CRC.maxCalcFiles")
    val maxSizeFiles: Long = config.underlying.getBytes("CRC.maxSizeFiles")

    logger.info(s"calcCRC maxSizeFiles = $maxSizeFiles   maxCalcFiles = $maxCalcFiles")

    database.runAsync(Tables.SmFileCard
      .filter(_.storeName === device)
      .filter(_.sha256.isEmpty)
      .filter(size => size.fSize.>(0L) && size.fSize.<=(maxSizeFiles))
      .sortBy(_.fParent.asc)
      .take(maxCalcFiles)
      .result).map { rowSeq =>

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
                database.runAsync(update).map(_ => logger.debug(s"calcCRC Set sha256 for key ${row.id}   path ${row.fParent} ${row.fName}"))
              }
            } catch {
              case _: java.io.FileNotFoundException | _: java.io.IOException => None
            }
          }
          logger.info(s"calcCRC Done for device = $device")

        case Failure(ex) => logger.error(s"calcCRC error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
      }
      Ok("Job run")
    }
  }
}

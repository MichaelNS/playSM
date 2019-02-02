package controllers

import java.time.LocalDateTime
import java.util

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.SmDevice
import models.db.Tables
import play.api.Logger
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.OsConf
import ru.ns.tools.FileUtils
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by ns on 12.03.2018
  */
@Singleton
class SmSyncStream @Inject()(val database: DBService)
  extends InjectedController {

  private val logger = Logger(classOf[SmSyncStream])

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def refreshDevice: Action[AnyContent] = Action.async {
    database.runAsync(Tables.SmDevice.sortBy(_.uid).to[List].map(_.uid).result).map { rowSeq =>
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

  def syncDevice(deviceUid: String): Action[AnyContent] = Action.async {
    debugParam
    FileUtils.getDevicesInfo(deviceUid).map { devices =>
      debug(devices)
      val device = devices.find(_.uuid == deviceUid)
      debug(device)
      if (device.isDefined) {
        val config = ConfigFactory.load("scanImport.conf")

        try {
          val impPath: util.List[String] = config.getStringList("paths2Scan.volumes." + deviceUid)
          debug(s"${impPath.size()} : $impPath")
          logger.debug(pprint.apply(impPath.asScala.map(c => device.get.mountpoint + OsConf.fsSeparator + c)).toString())
          Source.fromIterator(() => impPath.asScala.toIterator).buffer(1, OverflowStrategy.backpressure).map(path =>
            mergeDrive2Db(
              deviceUid,
              device.get.mountpoint,
              path,
              sExclusionDir = config.getStringList("paths2Scan.exclusionPath"),
              config.getStringList("paths2Scan.exclusionFile")
            ))
            .runWith(Sink.ignore)
            .onComplete {
              case Success(_) =>
                database.runAsync((for {uRow <- Tables.SmDevice if uRow.uid === deviceUid} yield uRow.syncDate).update(LocalDateTime.now()))
                  .map(_ => logger.info(s"Updated device $deviceUid"))
                Redirect(routes.SmApplication.deviceIndex(deviceUid))
              case Failure(ex) =>
                logger.error(s"syncDevice error: ${ex.toString}")
                Redirect(routes.SmApplication.smIndex())
            }
          //        Redirect(routes.SmApplication.deviceIndex(deviceUid))
          Redirect(routes.SmSync.deviceImport())

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

  def mergeDrive2Db(deviceUid: String,
                    mountPoint: String,
                    impPath: String,
                    sExclusionDir: util.List[String],
                    sExclusionFile: util.List[String]
                   ): Future[(Long, Long, Future[Int])] = {
    val funcName = "mergeDrive2Db"
    val start = System.currentTimeMillis
    val lstToIns = ArrayBuffer[Tables.SmFileCard#TableElementType]()

    val hSmBoFileCard = FileUtils.readDirRecursive(impPath, deviceUid, mountPoint, sExclusionDir, sExclusionFile)
    val hInMap = database.runAsync(Tables.SmFileCard
      .filter(_.storeName === deviceUid).filter(_.fParent === impPath)
      .unionAll(Tables.SmFileCard.filter(_.storeName === deviceUid).filter(_.fParent startsWith impPath + OsConf.fsSeparator)
      ).map(fld => (fld.id, fld.fLastModifiedDate)).to[List].result)
      .map { dbGet =>
        val hInMap: Map[String, List[(String, LocalDateTime)]] = dbGet.groupBy(_._1)
        logger.debug(s"$funcName -> path = [$impPath]  hSmBoFileCard.size = [${hSmBoFileCard.size}]  rowSeq.size = [${dbGet.size}]   get 2 lists time: ${System.currentTimeMillis - start} ms")
        hInMap
      }
    val resMerge = hInMap.map { hInMap =>
      hSmBoFileCard.foreach { value => // add FC
        if (!hInMap.contains(value.id)) {
          lstToIns += Tables.SmFileCardRow(value.id, value.storeName, value.fParent, value.fName, value.fExtension,
            value.fCreationDate, value.fLastModifiedDate, value.fSize, value.fMimeTypeJava, None, value.fNameLc)
        } else { // upd FC
          if (hInMap(value.id).head._2 != value.fLastModifiedDate) {
            database.runAsync(
              (for {uRow <- Tables.SmFileCard if uRow.id === value.id} yield (uRow.sha256, uRow.fCreationDate, uRow.fLastModifiedDate, uRow.fSize))
                .update((None, value.fCreationDate, value.fLastModifiedDate, value.fSize))
            ).map { _ => logger.debug(s"$funcName -> path = [$impPath]   Updated key ${value.id}") }
          }
        }
      }
      delFromDb(hInMap.keys.toSeq, hSmBoFileCard.map(p => p.id), impPath)
      //      logger.info(s"$funcName end -> path = [$impPath]   Elapsed time: ${System.currentTimeMillis - start} ms")
      (start, System.currentTimeMillis - start, database.runAsync((Tables.SmFileCard returning Tables.SmFileCard.map(_.id)) ++= lstToIns).map(_.size))
    }
    resMerge.onComplete {
      case Success(res) =>
        res._3.onComplete {
          case Success(cntIns) => logger.debug(s"mergeDrive2Db inserted [$cntIns] rows   path = [$impPath]   " + s"Elapsed time: ${res._2} ms " + s"All time: ${System.currentTimeMillis - res._1} ms ") //cntIns
          case Failure(ex) => logger.error(s"mergeDrive2Db 1 error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
        }
      case Failure(ex) => logger.error(s"mergeDrive2Db 2 error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
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

    logger.info(s"delFromDb - is done, path = [$path] "
      + s"idDb.size = [${idDb.size}]  idDevice.size = [${idDevice.size}]   delDiff.size = ${delDiff.size}"
      + s"  Elapsed time: ${System.currentTimeMillis - start} ms")
  }

  def calcCRC(device: String): Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxCalcFiles = config.getBytes("CRC.maxCalcFiles")
    val maxSizeFiles: Long = config.getBytes("CRC.maxSizeFiles")

    logger.info(s"calcCRC maxSizeFiles = $maxSizeFiles   maxCalcFiles = $maxCalcFiles")

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

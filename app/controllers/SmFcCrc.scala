package controllers

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Sink, Source}
import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.mvc._
import play.api.{Configuration, Logger}
import ru.ns.model.OsConf
import ru.ns.tools.FileUtils
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by ns on 05.01.2020
  */
@Singleton
class SmFcCrc @Inject()(cc: MessagesControllerComponents, config: Configuration, val database: DBService)
  extends MessagesAbstractController(cc) {

  val logger: Logger = play.api.Logger(getClass)

  implicit val system: ActorSystem = ActorSystem()

  def calcCRC(device: String): Action[AnyContent] = Action.async {
    val funcName = "calcCRC"

    FileUtils.getDevicesInfo(device).onComplete {
      case Success(mntDevices) =>
        getDevicesForCalcCrc(mntDevices).onComplete {
          case Success(devices) => devices.foreach(dbDevice => calcCrcByDevice(dbDevice._1, mntDevices))
          case Failure(e) => logger.error(s"Error $funcName getDevicesForCalcCrc, error = ${e.toString}\nStackTrace:\n ${e.getStackTrace.mkString("\n")}")
        }
      case Failure(e) => logger.error(s"Error $funcName getDevicesInfo , error = ${e.toString}\nStackTrace:\n ${e.getStackTrace.mkString("\n")}")
    }

    Future.successful(Ok("run calcCRC"))
  }

  def calcAllCRCActor(): Unit = {
    val funcName = "calcAllCRCActor"

    val maxDevicesCalc = config.get[Int]("CRC.maxDevicesCalc")

    FileUtils.getDevicesInfo().onComplete {
      case Success(mntDevices) =>
        getDevicesForCalcCrc(mntDevices).onComplete {
          case Success(devices) =>
            //            debug(devices)
            val cntScan: Int = devices.count(_._2)
            if (cntScan < maxDevicesCalc) {
              //              devices.filter(_._2 == false).take(maxDevicesCalc)
              //                .foreach(dbDevice => calcCrcByDevice(dbDevice._1, mntDevices))

              Source.fromIterator(() => devices.filter(!_._2).iterator)
                .throttle(elements = 1, 10.millisecond, maximumBurst = 1, mode = ThrottleMode.Shaping)
                .mapAsync(maxDevicesCalc) { device =>
                  calcCrcByDevice(device._1, mntDevices)
                }.runWith(Sink.ignore).onComplete {
                case Success(_) =>
                //                  logger.info(s"$funcName done calcCrcByDevice")
                case Failure(ex) =>
                  logger.error(s"syncDevice error: ${ex.toString}")
              }

            } else {
              logger.debug(s"$funcName skip scan because cntScan = $cntScan")
            }
          case Failure(e) => logger.error(s"Error $funcName getDevicesForCalcCrc, error = ${e.toString}\nStackTrace:\n ${e.getStackTrace.mkString("\n")}")
        }
      case Failure(e) => logger.error(s"Error $funcName getDevicesInfo , error = ${e.toString}\nStackTrace:\n ${e.getStackTrace.mkString("\n")}")
    }
  }

  def calcAllCRC(): Action[AnyContent] = Action.async {
    calcAllCRCActor()

    //    for (i <- 1 to 10) calcAllCRCActor()
    Future.successful(Ok("run calcAllCRC"))
  }

  def getDevicesForCalcCrc(mntDevices: scala.collection.mutable.ArrayBuffer[ru.ns.model.Device]): Future[Seq[(String, Boolean)]] = {
    //    val funcName = "getDevicesForCalcCrc"
    //    logger.debug(s"$funcName mntDevices = $mntDevices")

    database.runAsync(Tables.SmDevice
      .filter(job => job.visible === true && job.jobCalcExif === false && job.jobPathScan === false && job.jobResize === false &&
        job.uid.inSet(mntDevices.map(_.uuid))
      )
      .map(fld => (fld.uid, fld.jobCalcCrc))
      .result)
  }

  def calcCrcByDevice(deviceUid: String, mntDevices: scala.collection.mutable.ArrayBuffer[ru.ns.model.Device]): Future[(Seq[Any], Int)] = {
    for {
      startJob <- setJobCalcCrc(deviceUid, status = true)
      calc <- calcCrcByDeviceDb(deviceUid: String, mntDevices: scala.collection.mutable.ArrayBuffer[ru.ns.model.Device])
      endJob <- setJobCalcCrc(deviceUid, status = false, calc.length)
    } yield (calc, endJob)
  }

  def calcCrcByDeviceDb(deviceUid: String, mntDevices: scala.collection.mutable.ArrayBuffer[ru.ns.model.Device]): Future[Seq[Any]] = {
    val maxCalcFiles: Long = config.get[Long]("CRC.maxCalcFiles")
    val maxSizeFiles: Long = config.underlying.getBytes("CRC.maxSizeFiles")

    val calcCrcRes = database.runAsync(
      (for {
        (devRow, fcRow) <- Tables.SmDevice.join(Tables.SmFileCard).on((device, fc) => {
          device.uid === fc.deviceUid
        }) if devRow.uid === deviceUid && devRow.jobPathScan === false &&
          fcRow.sha256.isEmpty && fcRow.fSize > 0L && fcRow.fSize <= maxSizeFiles
      } yield (fcRow.id, fcRow.fParent, fcRow.fName, fcRow.fSize)
        )
        .sortBy(_._4.asc).take(maxCalcFiles).result).map { rowSeq =>
      val mountPoint = mntDevices.filter(_.uuid == deviceUid).head.mountpoint
      val sss = rowSeq.map { row => calcCrcByFile(mountPoint, row)
      }
      sss
    }
    calcCrcRes
  }

  /**
    * calcCrcByFile
    *
    * @param mountPoint mountPoint
    * @param row        row
    * @return
    */
  def calcCrcByFile(mountPoint: String, row: (String, String, String, Option[Long])): Any = {
    try {
      val sha = FileUtils.getGuavaSha256(mountPoint + OsConf.fsSeparator + row._2 + row._3)
      if (sha != "") {
        val update = {
          val q = for (uRow <- Tables.SmFileCard if uRow.id === row._1) yield uRow.sha256
          q.update(Some(sha))
        }
        database.runAsync(update).map(_ => logger.debug(s"calcCRC Set sha256 for key ${row._1}   path ${row._3} ${row._2}"))
      }
    } catch {
      case _: java.io.FileNotFoundException | _: java.io.IOException => None
    }
  }

  def setJobCalcCrc(device: String, status: Boolean, length: Int = -1): Future[Int] = {
    val funcName = "setJobCalcCrc"
    //    logger.debug(s"$funcName device = $device   $status")

    val update = {
      val q = for (uRow <- Tables.SmDevice if uRow.uid === device) yield (uRow.jobCalcCrc, uRow.crcDate)
      q.update((status, Some(LocalDateTime.now())))
    }
    database.runAsync(update)
  }

}

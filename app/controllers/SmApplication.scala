package controllers

import java.time.LocalDateTime

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.DeviceView
import models.db.Tables
import play.api.Logger
import play.api.mvc._
import ru.ns.model.OsConf
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ns on 23.01.2017.
  */
@Singleton
class SmApplication @Inject()(implicit assetsFinder: AssetsFinder, val database: DBService)
  extends InjectedController {

  val logger: Logger = play.api.Logger(getClass)

  def smIndex: Action[AnyContent] = Action.async {

    val qry =
      (Tables.SmDevice
        .joinLeft(Tables.SmFileCard) on ((device, fc) => {
        device.uid === fc.deviceUid && fc.sha256.isEmpty && fc.fSize > 0L
      }))
        .filter { case (device, _) => device.visible }
        .groupBy { case (device, _) => (device.name, device.labelV, device.uid, device.description, device.pathScanDate, device.visible, device.reliable) }
        .map { case (device, cnt) => (
          device._1,
          device._2,
          device._3,
          device._4,
          device._5,
          device._6,
          device._7,
          cnt.map(_._2.map(_.id)).countDefined
        )
        }
        .sortBy(_._2)
    database.runAsync(qry.result).map { rowSeq =>
      //      logger.debug(pprint.apply(rowSeq).toString())


      val devices = ArrayBuffer[DeviceView]()
      rowSeq.foreach { p => devices += DeviceView(name = p._1, label = p._2, uid = p._3, description = p._4, syncDate = p._5, visible = p._6, reliable = p._7, withOutCrc = p._8) }

      Ok(views.html.smr_index(devices)())
    }
  }

  def deviceIndex(device: String): Action[AnyContent] = Action {
    Ok(views.html.smd_index(device, ConfigFactory.load("scanImport.conf").getInt("Category.maxFilesInDir"))())
  }

  def deviceTree(device: String): Action[AnyContent] = Action {
    Ok(views.html.tree(device, assetsFinder))
  }

  def getByDevice(device: String): Action[AnyContent] = Action.async {
    val maxRes = 200

    logger.info(s"smFileCards # maxRes=$maxRes | device = $device")

    database.runAsync(Tables.SmFileCard.filter(_.deviceUid === device).take(maxRes).map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate)).result).map { rowSeq =>
      Ok(views.html.filecards(None, None, rowSeq)())
    }
  }

  def getByDeviceByLastModifDate(device: String): Action[AnyContent] = Action.async {
    val maxRes = 200

    logger.info(s"smFileCards # maxRes=$maxRes | device = $device")

    database.runAsync(
      Tables.SmFileCard
        .filter(_.deviceUid === device)
        .filterNot(_.fParent endsWith "_files")
        .sortBy(_.fLastModifiedDate.desc)
        .map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate))
        .take(maxRes)
        .result
    ).map { rowSeq =>
      Ok(views.html.filecards(None, None, rowSeq)())
    }
  }

  def listStoreNameAndCnt: Action[AnyContent] = Action.async {
    database.runAsync(Tables.SmFileCard.groupBy(p => p.deviceUid)
      .map { case (storeName, cnt) => (storeName, cnt.map(_.deviceUid).length) }
      .sortBy(_._1)
      .result
    ).map { rowSeq =>
      Ok(views.html.storename(rowSeq)())
    }
  }

  def debugQry(device: String): Action[AnyContent] = Action.async {
    val path = "Downloads"
    val ctnRec = 100

    val qry = (for {
      uRow <- Tables.SmDevice if uRow.labelV === device
      v_fName <- Tables.SmFileCard if v_fName.deviceUid === uRow.labelV && v_fName.sha256.isEmpty && v_fName.fSize > 0L
    } yield (uRow, v_fName))
      .groupBy(uRow =>
        (uRow._1.labelV, uRow._1.pathScanDate))
      .map({
        case (uRow, cnt) =>
          (uRow, cnt.map(_._1.labelV).length)
      })
      .sortBy(_._1)

    database.runAsync(
      qry.result
    ).map { rowSeq =>
      logger.warn(rowSeq.head.toString())
    }


    database.runAsync(Tables.SmFileCard
      .filter(_.deviceUid === device).filter(_.fParent === path)
      .unionAll(Tables.SmFileCard
        .filter(_.deviceUid === device).filter(_.fParent startsWith path + OsConf.fsSeparator)
      )
      .map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate))
      .take(ctnRec)
      .result
    ).map { rowSeq =>
      Ok(views.html.filecards(None, None, rowSeq)())
    }
  }
}

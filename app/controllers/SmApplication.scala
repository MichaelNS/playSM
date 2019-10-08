package controllers

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.db.Tables
import models.{DeviceView, SmFileCard}
import org.joda.time.DateTime
import play.api.mvc._
import ru.ns.model.OsConf
import services.db.DBService
import slick.jdbc.GetResult
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ns on 23.01.2017.
  */
@Singleton
class SmApplication @Inject()(implicit assetsFinder: AssetsFinder, val database: DBService)
  extends InjectedController {

  val logger = play.api.Logger(getClass)

  def smIndex: Action[AnyContent] = Action.async {
    implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

    val qry = sql"""
      SELECT
        x2.name,
        x2.label,
        x2.uid,
        x2.describe,
        x2.sync_date,
        x2.reliable,
      (SELECT count(1) FROM sm_file_card x3 WHERE x3.store_name = x2.uid AND (x3.sha256 IS NULL)  AND (x3.f_size > 0))
      FROM sm_device x2
      where x2.visible is true
      ORDER BY x2.label
      """
      .as[(String, String, String, String, DateTime, Boolean, Int)]
    database.runAsync(qry).map { rowSeq =>
      //      logger.debug(pprint.apply(rowSeq).toString())

      val devices = ArrayBuffer[DeviceView]()
      rowSeq.foreach { p => devices += DeviceView(name = p._1, label = p._2, uid = p._3, describe = p._4, syncDate = p._5, visible = true, reliable = p._6, withOutCrc = p._7) }

      Ok(views.html.smr_index(devices))
    }
  }

  def deviceIndex(device: String): Action[AnyContent] = Action {
    Ok(views.html.smd_index(device, ConfigFactory.load("scanImport.conf").getInt("Category.maxFilesInDir")))
  }

  def deviceTree(device: String): Action[AnyContent] = Action {
    Ok(views.html.tree(device, assetsFinder))
  }

  def getByDevice(device: String): Action[AnyContent] = Action.async {
    val maxRes = 200

    logger.info(s"smFileCards # maxRes=$maxRes | device = $device")

    database.runAsync(Tables.SmFileCard.filter(_.storeName === device).take(maxRes).map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate)).result).map { rowSeq =>
      Ok(views.html.filecards(None, None, rowSeq))
    }
  }

  def getByDeviceByLastModifDate(device: String): Action[AnyContent] = Action.async {
    val maxRes = 200

    logger.info(s"smFileCards # maxRes=$maxRes | device = $device")

    database.runAsync(
      Tables.SmFileCard
        .filter(_.storeName === device)
        .filterNot(_.fParent endsWith "_files")
        .sortBy(_.fLastModifiedDate.desc)
        .map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate))
        .take(maxRes)
        .result
    ).map { rowSeq =>
      Ok(views.html.filecards(None, None, rowSeq))
    }
  }

  def listStoreNameAndCnt: Action[AnyContent] = Action.async {
    database.runAsync(Tables.SmFileCard.groupBy(p => p.storeName)
      .map { case (storeName, cnt) => (storeName, cnt.map(_.storeName).length) }
      .sortBy(_._1)
      .result
    ).map { rowSeq =>
      Ok(views.html.storename(rowSeq))
    }
  }

  def debugQry(device: String): Action[AnyContent] = Action.async {
    val path = "Downloads"
    val ctnRec = 100

    val qry = (for {
      uRow <- Tables.SmDevice if uRow.label === device
      v_fName <- Tables.SmFileCard if v_fName.storeName === uRow.label && v_fName.sha256.isEmpty && v_fName.fSize > 0L
    } yield (uRow, v_fName))
      .groupBy(uRow =>
        (uRow._1.label, uRow._1.syncDate))
      .map({
        case (uRow, cnt) =>
          (uRow, cnt.map(_._1.label).length)
      })
      .sortBy(_._1)

    database.runAsync(
      qry.result
    ).map { rowSeq =>
      logger.warn(rowSeq.head.toString())
    }


    database.runAsync(Tables.SmFileCard
      .filter(_.storeName === device).filter(_.fParent === path)
      .unionAll(Tables.SmFileCard
        .filter(_.storeName === device).filter(_.fParent startsWith path + OsConf.fsSeparator)
      )
      .map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate))
      .take(ctnRec)
      .result
    ).map { rowSeq =>
      Ok(views.html.filecards(None, None, rowSeq))
    }
  }
}

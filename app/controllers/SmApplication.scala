package controllers

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.{DeviceView, SmFileCard}
import models.db.Tables
import org.joda.time.DateTime
import play.api.Logger
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
class SmApplication @Inject()(val database: DBService)
  extends InjectedController {

  def smIndex: Action[AnyContent] = Action.async {
    implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

    val qry = sql"""
      SELECT
        x2."NAME",
        x2."LABEL",
        x2."UID",
        x2."DESCRIBE",
        x2."SYNC_DATE",
      (SELECT count(1) FROM "sm_file_card" x3 WHERE x3."STORE_NAME" = x2."UID" AND (x3."SHA256" IS NULL)  AND (x3."F_SIZE" > 0))
      FROM "sm_device" x2
      where x2."VISIBLE" is true
      ORDER BY x2."LABEL"
      """
      .as[(String, String, String, String, DateTime, Int)]
    database.runAsync(qry).map { rowSeq =>
      //      Logger.debug(pprint.apply(rowSeq).toString())

      val devices = ArrayBuffer[DeviceView]()
      rowSeq.foreach { p => devices += DeviceView(name = p._1, label = p._2, uid = p._3, describe = p._4, syncDate = p._5, visible = true, withOutCrc = p._6) }

      Ok(views.html.smr_index(devices))
    }
  }

  def deviceIndex(device: String): Action[AnyContent] = Action {
    Ok(views.html.smd_index(device, ConfigFactory.load("scanImport.conf").getInt("Category.maxFilesInDir")))
  }

  def getByDevice(device: String): Action[AnyContent] = Action.async {
    val maxRes = 200

    Logger.info(s"smFileCards # maxRes=$maxRes | device = $device")

    database.runAsync(Tables.SmFileCard.filter(_.storeName === device).take(maxRes).to[List].result).map { rowSeq =>
      val fcSeq = rowSeq.map(SmFileCard(_))
      Ok(views.html.filecards(fcSeq))
    }
  }

  def getByDeviceByLastModifDate(device: String): Action[AnyContent] = Action.async {
    val maxRes = 200

    Logger.info(s"smFileCards # maxRes=$maxRes | device = $device")

    database.runAsync(
      Tables.SmFileCard
        .filter(_.storeName === device)
        .filterNot(_.fParent endsWith "_files")
        .sortBy(_.fLastModifiedDate.desc)
        .take(maxRes)
        .to[List]
        .result
    ).map { rowSeq =>
      val fcSeq = rowSeq.map(SmFileCard(_))
      Ok(views.html.filecards(fcSeq))
    }
  }

  def listStoreNameAndCnt: Action[AnyContent] = Action.async {
    database.runAsync(Tables.SmFileCard.groupBy(p => p.storeName)
      .map { case (storeName, cnt) => (storeName, cnt.map(_.storeName).length) }
      .sortBy(_._1)
      .to[List].result)
      .map { rowSeq =>
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
      Logger.warn(rowSeq.head.toString())
    }


    database.runAsync(Tables.SmFileCard
      .filter(_.storeName === device).filter(_.fParent === path)
      .unionAll(Tables.SmFileCard
        .filter(_.storeName === device).filter(_.fParent startsWith path + OsConf.fsSeparator)
      )
      .take(ctnRec)
      .to[List]
      .result
    ).map { rowSeq =>
      val fcSeq = rowSeq.map(SmFileCard(_))
      Ok(views.html.filecards(fcSeq))
    }
  }
}

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

@Singleton
class SmDeduplicate @Inject()(implicit assetsFinder: AssetsFinder, val database: DBService)
  extends InjectedController {

  val logger: Logger = play.api.Logger(getClass)


  def deleteFilesIfExist(device: String, fParent: String): Action[AnyContent] = Action.async {

    val baseQry = for {
      a <- Tables.SmFileCard
      if a.sha256.nonEmpty && a.deviceUid === device && a.fParent === fParent + OsConf.fsSeparator && Tables.SmFileCard
        .filter(b => b.deviceUid === device && b.sha256 === a.sha256 && b.fName === a.fName && b.fParent =!= a.fParent)
        //        .filter(b => b.deviceUid === device && b.sha256 === a.sha256 && b.fName === a.fName)
        .filterNot(b => b.fParent endsWith "_files")
        .map(p => p.fName)
        .exists
    } yield (a.fParent, a.fName, a.fLastModifiedDate)

    val cnt = baseQry.length
    //    val filtAll = baseQry
    val filtQry = baseQry
      .sortBy(r => (r._1, r._2))

    val composedAction = for {cnt <- cnt.result
                              filtQry <- filtQry.result} yield (cnt, filtQry)

    database.runAsync(composedAction).map { rowSeq =>
      println(rowSeq._1)
      println(rowSeq._2.length)
      println(rowSeq._2)

      Ok("123")
      //      Ok(views.html.sm_chk_device_backup(rowSeq._1, rowSeq._2)())
    }
  }

}

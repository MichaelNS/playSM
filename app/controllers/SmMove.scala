package controllers

import java.io.IOException
import java.nio.file.InvalidPathException

import better.files.File
import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Constraints
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.{Device, OsConf}
import ru.ns.tools.FileUtils
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


case class FormCrMove(newPath: String)

object FormCrMove {
  val form = Form(mapping(
    "newPath" -> text.verifying(Constraints.nonEmpty)
  )(FormCrMove.apply)(FormCrMove.unapply))
}

/**
  * Move files operations inside [[ru.ns.model.Device]]
  *
  * @param database database
  */
@Singleton
class SmMove @Inject()(val database: DBService)
  extends InjectedController {

  /**
    * show list path from [[models.db.Tables.SmFileCard]]
    *
    * @param categoryType - for query row [[models.db.Tables.SmFileCard]]
    * @param description  - for query row [[models.db.Tables.SmFileCard]]
    * @return [[views.html.path_by_description]]
    */
  def listPathByDescription(categoryType: String, description: String): Action[AnyContent] = Action.async {
    val qry = sql"""
       SELECT
         x2."F_PARENT",
         (SELECT string_agg(DISTINCT x3."STORE_NAME", ', ')
          FROM sm_file_card x3
          WHERE x3."F_PARENT" = x2."F_PARENT"),
         (SELECT count(1)
          FROM sm_file_card x3
          WHERE x3."F_PARENT" = x2."F_PARENT"),
         (SELECT pm."PATH_TO"
          FROM sm_path_move pm
          WHERE pm."PATH_FROM" = x2."F_PARENT")
      FROM "sm_file_card" x2
        JOIN sm_category_fc category ON category."F_NAME" = x2."F_NAME" and category."ID" = x2."SHA256"
       WHERE
             category."CATEGORY_TYPE" = '#$categoryType'
         AND category."DESCRIPTION" = '#$description'
       GROUP BY x2."F_PARENT"
       ORDER BY 3 DESC
      """
      .as[(String, String, Int, String)]
    database.runAsync(qry).map { rowSeq =>
      val moveForm: Form[FormCrMove] = Form(
        mapping(
          "newPath" -> nonEmptyText
        )(FormCrMove.apply)(FormCrMove.unapply)
      )

      Ok(views.html.path_by_description(categoryType, description, rowSeq, moveForm))
    }
  }

  /**
    * View all paths by crc
    *
    * @param fParent path
    * @return
    */
  def listAllPathsByLstCrc(fParent: String): Action[AnyContent] = Action.async {
    val lstBackUpSha256 = Tables.SmFileCard
      .filter(_.fParent === fParent)
      .filter(_.sha256.nonEmpty)
      .map(_.sha256).distinct

    database.runAsync(
      Tables.SmFileCard
        .filter(_.sha256 in lstBackUpSha256)
        .groupBy(p => (p.storeName, p.fParent))
        .map({ case ((storename, fparent), cnt) => (storename, fparent, cnt.map(_.fParent).length) })
        .sortBy(_._2)
        .result)
      .map { rowSeq =>
        Ok(views.html.move_by_path(rowSeq))
      }
  }

  /**
    * Cancel job to move files
    * Call from [[views.html.path_by_description]] and [[listPathByDescription]]
    *
    * @see [[views.html.path_by_description]]
    * @see [[listPathByDescription]]
    * @param categoryType - category type for Redirect
    * @param description  - description for Redirect
    * @param device       - for remove row [[models.db.Tables.SmPathMove]]
    * @param path         - for remove row [[models.db.Tables.SmPathMove]]
    * @return Redirect 2 [[listPathByDescription]]
    **/
  def delJobToMove(categoryType: String, description: String, device: String, path: String): Action[AnyContent] = Action.async {
    val insRes = database.runAsync(Tables.SmPathMove.filter(_.storeName === device).filter(_.pathFrom === path).delete)
    insRes onComplete {
      case Success(suc) => Logger.debug(s"del [$suc] row - device = [$device]")
      case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
    }

    Future.successful(Redirect(routes.SmMove.listPathByDescription(categoryType, description)))
  }

  def createJobToMove(categoryType: String, description: String, device: String, oldPath: String): Action[AnyContent] = Action.async { implicit request =>
    FormCrMove.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.move_form(formWithErrors, categoryType, description, device, oldPath))),
      success = path => {
        Logger.warn(path.newPath)
        if (path.newPath.isEmpty) {
          val form = FormCrMove.form.fill(path).withError("newPath", " isEmpty")
          Future.successful(BadRequest(views.html.move_form(form, categoryType, description, device, oldPath)))
        } else {
          val cRow = Tables.SmPathMoveRow(-1, device, oldPath, path.newPath)
          debug(cRow)

          val insRes = database.runAsync((Tables.SmPathMove returning Tables.SmPathMove.map(_.id)) += cRow)
          insRes onComplete {
            case Success(insSuc) => Logger.debug(s"Inserted row move = $insSuc   $cRow")
            case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
          }
          debug(insRes)

          Future.successful(Redirect(routes.SmMove.listPathByDescription(categoryType, description)))
        }
      }
    )
  }

  /**
    * Get [[ru.ns.model.Device]] from [[ru.ns.tools.FileUtils.getDevicesInfo]] and start [[SmMove.moveByDevice]]
    *
    * @return Future String "run moveAllDevices"
    */
  def moveAllDevices: Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxJob: Int = config.getInt("Move.maxJob")
    val maxMoveFiles: Int = config.getInt("Move.maxMoveFiles")

    FileUtils.getDevicesInfo() onComplete {
      case Success(lstDevices) =>
        debug(lstDevices)

        for (device <- lstDevices) {
          moveByDevice(device = device, maxJob: Int, maxMoveFiles)
        }
      case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
    }

    Future.successful(Ok("run moveAllDevices"))
  }

  /**
    * Move files inside device
    *
    * @param device       - [[ru.ns.model.Device]] object
    * @param maxJob       - max rows from [[models.db.Tables.SmPathMove]]
    * @param maxMoveFiles - maxMoveFiles from [[models.db.Tables.SmFileCard]]
    * @return String "moveByDevice is DONE"
    */
  def moveByDevice(device: Device, maxJob: Int, maxMoveFiles: Int): String = {
    debugParam

    database.runAsync(Tables.SmPathMove.filter(_.storeName === device.uuid).take(maxJob).to[List].result).map { moveJobRow =>
      debug(moveJobRow)
      moveJobRow.foreach { rowMove =>
        val pathFrom = rowMove.pathFrom
        val pathTo = rowMove.pathTo

        debug(s"pathFrom = [$pathFrom]   pathTo = [$pathTo]")

        // TODO проверить, что соответствующий путь указан в конфиге для сканирования
        database.runAsync(Tables.SmFileCard
          .filter(_.storeName === rowMove.storeName)
          .filter(_.fParent === pathFrom)
          .take(maxMoveFiles)
          .to[List].result).map { rowFcSeq =>
          rowFcSeq.foreach { rowFc =>
            debug(rowFc)
            try {
              moveAction(rowFc, device.mountpoint, pathTo)
            } catch {
              case ex: InvalidPathException =>
                Logger.error(s"moveAction error = ${ex.toString}")
                throw ex
            }
          }
        }
        clearJob(idJob = rowMove.id, storeName = rowMove.storeName, mountPoint = device.mountpoint, pathFrom = pathFrom)
      }
    }
    Logger.info("moveByDevice is DONE")
    "moveByDevice is DONE"
  }

  /**
    * Move files by row [[models.db.Tables.SmFileCard]]
    *
    * @param rowFc      - row [[models.db.Tables.SmFileCard]]
    * @param mountPoint - [[ru.ns.model.Device]] mountPoint
    * @param pathTo     - new path
    * @return Any
    */
  def moveAction(rowFc: Tables.SmFileCard#TableElementType, mountPoint: String, pathTo: String): Any = {
    import com.roundeights.hasher.Implicits._

    val fileFrom = File(mountPoint + OsConf.fsSeparator + rowFc.fParent + rowFc.fName)
    val fileTo = File(mountPoint + OsConf.fsSeparator + pathTo + rowFc.fName)
    val dirTo = File(mountPoint + OsConf.fsSeparator + pathTo)

    Logger.info(s"fileName = ${rowFc.fName}   exists=${fileTo.exists()}")
    Logger.info(s"dirTo    = $dirTo   exists=${dirTo.exists()}")

    if (!fileTo.exists) {
      if (dirTo.exists() || dirTo.createDirectories().exists) {
        // get file from DB
        val cRow = rowFc.copy(id = (rowFc.storeName + pathTo + rowFc.fName).sha256.toUpperCase,
          fParent = pathTo)

        // insert
        val insRes = database.runAsync((Tables.SmFileCard returning Tables.SmFileCard.map(_.id)) += cRow)
        insRes onComplete {
          case Success(insSuc) => Logger.debug(s"Inserted row move = $insSuc   $cRow")
          case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
        }
        // move + delete
        try {
          fileFrom.moveTo(fileTo, overwrite = false)

          val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === rowFc.id).delete)
          insRes onComplete {
            case Success(suc) => Logger.debug(s"del [$suc] row , id = [$rowFc.id]")
            case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
          }
        } catch {
          case ex: IOException =>
            Logger.error(s"err move = ${ex.toString}")

            val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === rowFc.id).delete)
            insRes onComplete {
              case Success(suc) => Logger.debug(s"del [$suc] row , id = [$rowFc.id]")
              case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
            }
        }
      } else {
        Logger.warn(s"Can`t create path = ${mountPoint + dirTo}")
      }
    } else {
      Logger.warn(s"File exists = $fileTo")
    }
  }

  /**
    * Remove row [[models.db.Tables.SmPathMove]]
    * Check 0 row count from query [[models.db.Tables.SmFileCard]]
    *
    * @param idJob      - id row
    * @param storeName  - for get row [[models.db.Tables.SmFileCard]]
    * @param mountPoint - [[ru.ns.model.Device]] mountPoint
    * @param pathFrom   - for get row [[models.db.Tables.SmFileCard]]
    * @return String "clearJob is DONE"
    */
  def clearJob(idJob: Int, storeName: String, mountPoint: String, pathFrom: String): String = {
    debugParam

    database.runAsync(Tables.SmFileCard
      .filter(_.storeName === storeName)
      .filter(_.fParent === pathFrom)
      .length.result).map { rowFcCnt =>

      Logger.debug(s"clearJob - device = [$storeName] mountPoint = [$mountPoint] pathFrom = [$pathFrom]   rowFcCnt.size = $rowFcCnt")

      if (rowFcCnt == 0) {
        val dir = better.files.File(mountPoint + OsConf.fsSeparator + pathFrom)
        if (dir.entries.isEmpty) {
          try {
            dir.delete(true)

            val insRes = database.runAsync(Tables.SmPathMove.filter(_.id === idJob).delete)
            insRes onComplete {
              case Success(suc) => Logger.debug(s"del [$suc] row , id = [$idJob]")
              case Failure(t) => Logger.error(s"An error has occured: = ${t.getMessage}")
            }
          } catch {
            case ex: IOException => Logger.error(s"err delete = ${ex.toString}")
          }
        } else {
          Logger.warn(s"clearJob - Can`t remove = $pathFrom   ${dir.toString()}")
        }
      }
    }
    Logger.info("clearJob is DONE")
    "clearJob is DONE"
  }
}

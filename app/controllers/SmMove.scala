package controllers

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.InvalidPathException
import java.time.LocalDateTime

import better.files.File
import com.google.common.hash.Hashing
import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.db.Tables
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

  val logger = play.api.Logger(getClass)

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
         x2.f_parent,
         (SELECT string_agg(DISTINCT x3.device_uid, ', ')
          FROM sm_file_card x3
          WHERE x3.f_parent = x2.f_parent),
         (SELECT count(1)
          FROM sm_file_card x3
          WHERE x3.f_parent = x2.f_parent),
         (SELECT pm.path_to
          FROM sm_job_path_move pm
          WHERE pm.path_from = x2.f_parent)
      FROM sm_file_card x2
        JOIN sm_category_fc category ON category.f_name = x2.f_name and category.id = x2.sha256
       WHERE
             category.category_type = '#$categoryType'
         AND category.description = '#$description'
       GROUP BY x2.f_parent
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
        .groupBy(p => (p.deviceUid, p.fParent))
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
    * @param device       - for remove row [[models.db.Tables.SmJobPathMove]]
    * @param path         - for remove row [[models.db.Tables.SmJobPathMove]]
    * @return Redirect 2 [[listPathByDescription]]
    **/
  def delJobToMove(categoryType: String, description: String, device: String, path: String): Action[AnyContent] = Action.async {
    val insRes = database.runAsync(Tables.SmJobPathMove.filter(_.deviceUid === device).filter(_.pathFrom === path).delete)
    insRes onComplete {
      case Success(suc) => logger.debug(s"del [$suc] row - device = [$device]")
      case Failure(t) => logger.error(s"An error has occured: = ${t.getMessage}")
    }

    Future.successful(Redirect(routes.SmMove.listPathByDescription(categoryType, description)))
  }

  def createJobToMove(categoryType: String, description: String, device: String, oldPath: String): Action[AnyContent] = Action.async { implicit request =>
    FormCrMove.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.move_form(formWithErrors, categoryType, description, device, oldPath))),
      success = path => {
        logger.warn(path.newPath)
        if (path.newPath.isEmpty) {
          val form = FormCrMove.form.fill(path).withError("newPath", " isEmpty")
          Future.successful(BadRequest(views.html.move_form(form, categoryType, description, device, oldPath)))
        } else {
          val cRow = Tables.SmJobPathMoveRow(-1, device, oldPath, path.newPath)
          debug(cRow)

          val insRes = database.runAsync((Tables.SmJobPathMove returning Tables.SmJobPathMove.map(_.id)) += cRow)
          insRes onComplete {
            case Success(insSuc) => logger.debug(s"Inserted row move = $insSuc   $cRow")
            case Failure(t) => logger.error(s"An error has occured: = ${t.getMessage}")
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
      case Failure(t) => logger.error(s"An error has occured: = ${t.getMessage}")
    }

    Future.successful(Ok("run moveAllDevices"))
  }

  /**
    * Move files inside device
    *
    * @param device       - [[ru.ns.model.Device]] object
    * @param maxJob       - max rows from [[models.db.Tables.SmJobPathMove]]
    * @param maxMoveFiles - maxMoveFiles from [[models.db.Tables.SmFileCard]]
    * @return String "moveByDevice is DONE"
    */
  def moveByDevice(device: Device, maxJob: Int, maxMoveFiles: Int): String = {
    debugParam

    database.runAsync(Tables.SmJobPathMove.filter(_.deviceUid === device.uuid).take(maxJob).result).map { moveJobRow =>
      debug(moveJobRow)
      moveJobRow.foreach { rowMove =>
        val pathFrom = rowMove.pathFrom
        val pathTo = rowMove.pathTo

        debug(s"pathFrom = [$pathFrom]   pathTo = [$pathTo]")

        // TODO проверить, что соответствующий путь указан в конфиге для сканирования
        database.runAsync(Tables.SmFileCard
          .filter(_.deviceUid === rowMove.deviceUid)
          .filter(_.fParent === pathFrom)
          .take(maxMoveFiles)
          .result).map { rowFcSeq =>
          rowFcSeq.foreach { rowFc =>
            debug(rowFc)
            try {
              moveAction(rowFc, device.mountpoint, pathTo)
            } catch {
              case ex: InvalidPathException =>
                logger.error(s"moveAction error = ${ex.toString}")
                throw ex
            }
          }
        }
        clearJob(idJob = rowMove.id, storeName = rowMove.deviceUid, mountPoint = device.mountpoint, pathFrom = pathFrom)
      }
    }
    logger.info("moveByDevice is DONE")
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
    val fileFrom = File(mountPoint + OsConf.fsSeparator + rowFc.fParent + rowFc.fName)
    val fileTo = File(mountPoint + OsConf.fsSeparator + pathTo + rowFc.fName)
    val dirTo = File(mountPoint + OsConf.fsSeparator + pathTo)

    logger.info(s"fileName = ${rowFc.fName}   exists=${fileTo.exists()}")
    logger.info(s"dirTo    = $dirTo   exists=${dirTo.exists()}")

    if (!fileTo.exists) {
      if (dirTo.exists() || dirTo.createDirectories().exists) {
        // get file from DB
        val cRow = rowFc.copy(id = Hashing.sha256().hashString(rowFc.deviceUid + pathTo + rowFc.fName, StandardCharsets.UTF_8).toString.toUpperCase,
          fParent = pathTo)

        // insert
        val insRes = database.runAsync((Tables.SmFileCard returning Tables.SmFileCard.map(_.id)) += cRow)
        insRes onComplete {
          case Success(insSuc) => logger.debug(s"Inserted row move = $insSuc   $cRow")
          case Failure(t) => logger.error(s"An error has occured: = ${t.getMessage}")
        }
        // move + delete
        try {
          fileFrom.moveTo(fileTo)

          val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === rowFc.id).delete)
          insRes onComplete {
            case Success(suc) => logger.debug(s"del [$suc] row , id = [$rowFc.id]")
            case Failure(t) => logger.error(s"An error has occured: = ${t.getMessage}")
          }
        } catch {
          case ex: IOException =>
            logger.error(s"err move = ${ex.toString}")

            val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === rowFc.id).delete)
            insRes onComplete {
              case Success(suc) => logger.debug(s"del [$suc] row , id = [$rowFc.id]")
              case Failure(t) => logger.error(s"An error has occured: = ${t.getMessage}")
            }
        }
      } else {
        logger.warn(s"Can`t create path = ${mountPoint + dirTo}")
      }
    } else {
      logger.warn(s"File exists = $fileTo")
    }
  }

  /**
    * Remove row [[models.db.Tables.SmJobPathMove]]
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
      .filter(_.deviceUid === storeName)
      .filter(_.fParent === pathFrom)
      .length.result).map { rowFcCnt =>

      logger.debug(s"clearJob - device = [$storeName] mountPoint = [$mountPoint] pathFrom = [$pathFrom]   rowFcCnt.size = $rowFcCnt")

      if (rowFcCnt == 0) {
        val dir = better.files.File(mountPoint + OsConf.fsSeparator + pathFrom)
        if (dir.entries.isEmpty) {
          try {
            dir.delete(swallowIOExceptions = true)

            database.runAsync((for {uRow <- Tables.SmJobPathMove if uRow.id === idJob} yield uRow.done)
              .update(Some(LocalDateTime.now())))
              .map(_ => logger.info(s"done for move job complete for device $idJob"))
          } catch {
            case ex: IOException => logger.error(s"err delete = ${ex.toString}")
          }
        } else {
          logger.warn(s"clearJob - Can`t remove = $pathFrom   ${dir.toString()}")
        }
      }
    }
    logger.info("clearJob is DONE")
    "clearJob is DONE"
  }
}

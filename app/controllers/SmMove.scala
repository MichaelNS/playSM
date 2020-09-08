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
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Constraints
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.{Device, OsConf}
import ru.ns.tools.FileUtils
import services.db.DBService
import slick.sql.SqlStreamingAction
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


case class FormCrMove(newPath: String)

object FormCrMove {
  val form: Form[FormCrMove] = Form(mapping(
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

  val logger: Logger = play.api.Logger(getClass)

  /**
    * show list path from [[models.db.Tables.SmFileCard]]
    *
    * @param categoryType - for query row [[models.db.Tables.SmFileCard]]
    * @param category     - for query row [[models.db.Tables.SmFileCard]]
    * @return [[views.html.category.path_by_category]]
    */
  def listPathByCategory(categoryType: String, category: String, subCategory: String): Action[AnyContent] = Action.async {
    debugParam
    val qry = sql"""
       SELECT
         x2.f_parent,
         (SELECT string_agg(DISTINCT sm_device.label_v, ', ')
          FROM sm_file_card x3
              JOIN sm_device ON sm_device.uid = x3.device_uid
          WHERE x3.f_parent = x2.f_parent
          ),
         (SELECT count(1)
          FROM sm_file_card x3
          WHERE x3.f_parent = x2.f_parent),
         (SELECT pm.path_to
          FROM sm_job_path_move pm
          WHERE pm.path_from = x2.f_parent)
      FROM sm_file_card x2
        JOIN sm_category_fc category ON category.f_name = x2.f_name and category.sha256 = x2.sha256
        JOIN sm_category_rule category_rule ON category_rule.id = category.id
       WHERE
             category_rule.category_type = '#$categoryType'
         AND category_rule.category = '#$category'
         AND category_rule.sub_category = '#$subCategory'
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

      Ok(views.html.category.path_by_category(categoryType, category, subCategory, rowSeq, moveForm)())
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
    // TODO fix to exist see SmReport.checkBackUp
    database.runAsync(
      Tables.SmFileCard
        .filter(_.sha256 in lstBackUpSha256)
        .groupBy(p => (p.deviceUid, p.fParent))
        .map({ case ((storename, fparent), cnt) => (storename, fparent, cnt.map(_.fParent).length) })
        .sortBy(_._2)
        .result)
      .map { rowSeq =>
        Ok(views.html.move_by_path(rowSeq)())
      }
  }

  /**
    * Cancel job to move files
    * Call from [[views.html.category.path_by_category]] and [[listPathByCategory()]]
    *
    * @see [[views.html.category.path_by_category]]
    * @see [[listPathByCategory]]
    * @param categoryType - category type for Redirect
    * @param category     - category for Redirect
    * @param subCategory  - subCategory for Redirect
    * @param device       - for remove row [[models.db.Tables.SmJobPathMove]]
    * @param path         - for remove row [[models.db.Tables.SmJobPathMove]]
    * @return Redirect 2 [[listPathByCategory]]
    **/
  def delJobToMove(categoryType: String, category: String, subCategory: String, device: String, path: String): Action[AnyContent] = Action.async {
    val insRes = database.runAsync(Tables.SmJobPathMove.filter(_.deviceUid === device).filter(_.pathFrom === path).delete)
    insRes onComplete {
      case Success(suc) => logger.debug(s"del [$suc] row - device = [$device]")
      case Failure(t) => logger.error(s"An error has occurred: = ${t.getMessage}")
    }

    Future.successful(Redirect(routes.SmMove.listPathByCategory(categoryType, category, subCategory)))
  }

  def createJobToMove(categoryType: String, category: String, subCategory: String, device: String, oldPath: String): Action[AnyContent] = Action.async { implicit request =>
    FormCrMove.form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.html.move_form(formWithErrors, categoryType, category, subCategory, device, oldPath)())),
      success = path => {
        logger.warn(path.newPath)
        if (path.newPath.isEmpty) {
          val form = FormCrMove.form.fill(path).withError("newPath", " isEmpty")
          Future.successful(BadRequest(views.html.move_form(form, categoryType, category, subCategory, device, oldPath)()))
        } else {
          val cRow = Tables.SmJobPathMoveRow(-1, device, oldPath, path.newPath)
          debug(cRow)

          val insRes = database.runAsync((Tables.SmJobPathMove returning Tables.SmJobPathMove.map(_.id)) += cRow)
          insRes onComplete {
            case Success(insSuc) => logger.debug(s"Inserted row move = $insSuc   $cRow")
            case Failure(t) => logger.error(s"An error has occurred: = ${t.getMessage}")
          }
          debug(insRes)

          Future.successful(Redirect(routes.SmMove.listPathByCategory(categoryType, category, subCategory)))
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
      case Failure(t) => logger.error(s"An error has occurred: = ${t.getMessage}")
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
        closeJob(idJob = rowMove.id, storeName = rowMove.deviceUid, mountPoint = device.mountpoint, pathFrom = pathFrom)
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
          case Failure(t) => logger.error(s"An error has occurred: = ${t.getMessage}")
        }
        // move + delete
        try {
          fileFrom.moveTo(fileTo)

          val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === rowFc.id).delete)
          insRes onComplete {
            case Success(suc) => logger.debug(s"del [$suc] row , id = [$rowFc.id]")
            case Failure(t) => logger.error(s"An error has occurred: = ${t.getMessage}")
          }
        } catch {
          case ex: IOException =>
            logger.error(s"err move = ${ex.toString}")

            val insRes = database.runAsync(Tables.SmFileCard.filter(_.id === rowFc.id).delete)
            insRes onComplete {
              case Success(suc) => logger.debug(s"del [$suc] row , id = [$rowFc.id]")
              case Failure(t) => logger.error(s"An error has occurred: = ${t.getMessage}")
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
    * Update row [[models.db.Tables.SmJobPathMove]]
    * Check 0 row count from query [[models.db.Tables.SmFileCard]]
    *
    * @param idJob      - id row
    * @param storeName  - for get row [[models.db.Tables.SmFileCard]]
    * @param mountPoint - [[ru.ns.model.Device]] mountPoint
    * @param pathFrom   - for get row [[models.db.Tables.SmFileCard]]
    * @return String "clearJob is DONE"
    */
  def closeJob(idJob: Int, storeName: String, mountPoint: String, pathFrom: String): String = {
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

  // find . -type d -empty -delete
  def runMoveFilesBySource(): Action[AnyContent] = Action.async {
    val deviceUidSource: String = ""
    val deviceUidTarget: String = ""

    moveFilesBySource(deviceUidSource, deviceUidTarget)
    Future.successful(Ok("run moveFilesBySource"))
  }

  def moveFilesBySource(deviceUidSource: String, deviceUidTarget: String): Future[Vector[Any]] = {
    val resDb = (for {
      fileCards <- database.runAsync(getFilesForMoveFilesBySource(deviceUidSource, deviceUidTarget))
      mount <- FileUtils.getDeviceInfo(deviceUidTarget)
    } yield (fileCards, mount))

    resDb.filter { case (_, device) => device.isDefined }.map { case (fileCards, deviceOpt) =>
      val device = deviceOpt.get
      fileCards.filter(_._1 == 1).map { fc =>

      moveAction2(device.mountpoint, fParent = fc._6, fc._3, pathTo = fc._5)
      }
    }
  }

  def moveAction2(mountPoint: String, fParent: String, fName: String, pathTo: String): Any = {
    val fileFrom = File(mountPoint + OsConf.fsSeparator + fParent + fName)
    val fileTo = File(mountPoint + OsConf.fsSeparator + pathTo + fName)
    val dirTo = File(mountPoint + OsConf.fsSeparator + pathTo)

    if (!fileTo.exists) {
      if (dirTo.exists() || dirTo.createDirectories().exists) {
        // move + delete
        try {
          fileFrom.moveTo(fileTo)
        } catch {
          case ex: IOException => logger.error(s"err move = ${ex.toString}")
        }
      } else {
        logger.warn(s"Can`t create path = ${mountPoint + dirTo}")
      }
    } else {
      fileFrom.delete()
    }
  }

  def getFilesForMoveFilesBySource(deviceUidSource: String, deviceUidTarget: String): SqlStreamingAction[Vector[(Int, String, String, String, String, String)], (Int, String, String, String, String, String), Effect] = {
    debugParam

    val qry = sql"""
        SELECT (SELECT COUNT(1)
                FROM sm_file_card x2
                WHERE x2.f_name = x1.f_name
                  AND x2.sha256 = x1.sha256
                  AND x2.device_uid = x1.device_uid),
               (SELECT ARRAY_AGG(x2.f_parent)
                FROM sm_file_card x2
                WHERE x2.f_name = x1.f_name
                  AND x2.sha256 = x1.sha256
                  AND x2.device_uid = x1.device_uid),
               x1.f_name,
               x1.sha256,
               x1.f_parent,
               x2.f_parent
        FROM sm_file_card x1
                 INNER JOIN sm_file_card x2
                            ON x2.f_name = x1.f_name
                                AND x2.sha256 = x1.sha256
                                AND x1.id != x2.id
                                AND x2.device_uid != x1.device_uid
                                AND x1.f_parent != x2.f_parent
        WHERE x1.device_uid = '#$deviceUidSource'
          and x2.device_uid = '#$deviceUidTarget'
        ORDER BY x1.f_parent,
                 x2.f_parent,
                 x1.f_name

      """
      .as[(Int, String, String, String, String, String)]

    qry
  }

}

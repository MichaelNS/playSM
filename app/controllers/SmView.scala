package controllers

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.Logger
import play.api.http.HttpEntity
import play.api.mvc._
import ru.ns.model.{Device, OsConf}
import ru.ns.tools.FileUtils
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


@Singleton
class SmView @Inject()(val database: DBService)
  extends InjectedController {

  private val logger = Logger(classOf[SmView])

  def viewStorage(deviceName: String, depth: Int = 1): Action[AnyContent] = Action.async {
    debugParam

    val qry = sql"""
       select distinct split_part(fc."F_PARENT", '/', #$depth),
                       cast(sum("F_SIZE") / 1024 /1024 as int),
                       count(1),
                       count(1) filter (where sm_category_fc is null),
                       array_agg(DISTINCT sm_category_fc."CATEGORY_TYPE") filter (where sm_category_fc is not null)
       FROM "sm_file_card" fc
              left outer join sm_category_fc on fc."SHA256" = sm_category_fc."ID"
       where fc."STORE_NAME" = '#$deviceName'
       and fc."F_SIZE" > 0
       GROUP BY split_part(fc."F_PARENT", '/', #$depth)
       ORDER BY split_part(fc."F_PARENT", '/', #$depth)
      """
      .as[(String, Int, Int, Int, String)]

    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.smd_explorer(deviceName, rowSeq, depth + 1))
    }
  }

  def viewFile(sha256: Option[String]): Action[AnyContent] = Action.async {
    val result: Future[(Seq[(String, String, String, Option[String])], ArrayBuffer[Device])] = for {
      lstFiles <- getFilesFromSha256(sha256)
      lstDevices <- FileUtils.getDevicesInfo()
    } yield (lstFiles, lstDevices)

    val res = Await.result(result, 10.seconds)
    val mountPoints = ArrayBuffer[String]()

    if (res._1.nonEmpty) {
      val resFc = res._1.head
      res._2.foreach { device =>
        if (device.uuid == resFc._1) {
          mountPoints += device.mountpoint
        }
      }
      logger.debug(s"mountPoints=$mountPoints")
      Future.successful(openFile(mountPoints.head, resFc._2, resFc._3, resFc._4))
    }
    else {
      Future.successful(BadRequest(s"can't show file with ID sha256 = ${sha256.getOrElse("")}"))
    }
  }

  def viewFileByNaturalKey(deviceUid: String, path: String, fName: String): Action[AnyContent] = Action.async {
    val result: Future[(Seq[(String, String, String, Option[String])], ArrayBuffer[Device])] = for {
      lstFiles <- getFilesByNaturalKey(deviceUid, path, fName)
      lstDevices <- FileUtils.getDevicesInfo()
    } yield (lstFiles, lstDevices)

    val res = Await.result(result, 10.seconds)
    val mountPoints = ArrayBuffer[String]()

    if (res._1.nonEmpty) {
      val resFc = res._1.head
      res._2.foreach { device =>
        if (device.uuid == resFc._1) {
          mountPoints += device.mountpoint
        }
      }
      logger.debug(s"mountPoints=$mountPoints")
      Future.successful(openFile(mountPoints.head, resFc._2, resFc._3, resFc._4))
    }
    else {
      Future.successful(BadRequest(s"can't show file with NaturalKey = $path $fName"))
    }
  }

  /**
    * get files by sha256
    * used [[SmView.viewFile]]
    *
    * @param sha256 sha256
    * @return list files
    */
  def getFilesFromSha256(sha256: Option[String]): Future[List[(String, String, String, Option[String])]] = {
    val rowSeq = database.runAsync(Tables.SmFileCard
      .filter(_.sha256 === sha256)
      .map(fc => (fc.storeName, fc.fParent, fc.fName, fc.fMimeTypeJava)).to[List].result)
      .map(rowSeq => rowSeq)

    rowSeq
  }

  /**
    * get files by NaturalKey
    * used [[SmView.viewFile]]
    *
    * @param deviceUid deviceUid
    * @param path      path
    * @param fName     fName
    * @return list files
    */
  def getFilesByNaturalKey(deviceUid: String, path: String, fName: String): Future[List[(String, String, String, Option[String])]] = {
    val rowSeq = database.runAsync(Tables.SmFileCard
      .filter(_.storeName === deviceUid)
      .filter(_.fParent === path)
      .filter(_.fName === fName)
      .map(fc => (fc.storeName, fc.fParent, fc.fName, fc.fMimeTypeJava)).to[List].result)
      .map(rowSeq => rowSeq)

    rowSeq
  }

  /**
    * open file in browser
    * used [[SmView.viewFile]]
    *
    * @param mountPoint mountPoint
    * @param fPath      file path
    * @param name       file name
    * @param mimeType   mimeType
    * @return
    */
  def openFile(mountPoint: String, fPath: String, name: String, mimeType: Option[String]): Result = {
    val fullPath: String = mountPoint + OsConf.fsSeparator + fPath + name
    debug(fullPath)
    val file = new java.io.File(fullPath)
    val path: java.nio.file.Path = file.toPath
    val source: Source[ByteString, _] = FileIO.fromPath(path)

    val contentLength = Some(file.length())

    Result(
      header = ResponseHeader(OK, Map.empty),
      body = HttpEntity.Streamed(source, contentLength, mimeType)
    )
  }

  def openFileServing(mountPoint: String, fPath: String, name: String): Action[AnyContent] = Action {
    debugParam

    val fullPath: String = mountPoint + OsConf.fsSeparator + fPath + name
    debug(fullPath)

    Ok.sendFile(
      content = new java.io.File(fullPath),
      inline = true
    )
  }

  def viewPathBySha256(sha256: String): Action[AnyContent] = Action.async {

    val qry = sql"""
       SELECT
         "F_NAME",
         "F_PARENT",
         "STORE_NAME"
       FROM "sm_file_card" card
       WHERE "SHA256" = '#$sha256'
      """
      .as[(String, String, String)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.fc_by_sha256(sha256, rowSeq))
    }
  }


  @deprecated(message = "viewStorage", since = "2018-10-14")
  def viewStorageOld(deviceName: String, depth: Int = 0): Action[AnyContent] = Action.async {
    debugParam

    val qry = sql"""
      select distinct ("F_PARENT")
      from sm_file_card
      where "STORE_NAME" = '#$deviceName';
      """
      .as[String]

    database.runAsync(qry).map { rowSeq =>
      logger.info(s"viewStorage rowSeq.size = ${rowSeq.size}")

      val dirs = scala.collection.mutable.SortedSet[String]()

      rowSeq.sorted.foreach { dir =>
        val spDirs = dir.split("/")
        val spDir = spDirs.lift(depth)

        if (spDir.isDefined) {
          dirs += spDir.get
        }
      }

      // for correct sort in view.html
      var folders = Vector[String]()
      dirs.foreach { q =>
        folders = folders :+ q
      }

      logger.info(s"viewStorage folders =\n${folders.mkString("\n")}")
      //      Ok(views.html.smd_explorer(deviceName, folders, depth + 1))
      Ok("")
    }
  }
}

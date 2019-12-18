package controllers


import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.mvc._
import ru.ns.model.OsConf
import ru.ns.tools.{FileUtils, SmImageUtil}
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Created by ns on 29.10.2019
  */
@Singleton
class SmImage @Inject()(config: Configuration, val database: DBService)
  extends InjectedController {

  val logger = play.api.Logger(getClass)
  val maxResult: Int = config.get[Int]("Images.maxResult")
  val pathCache: String = config.get[String]("Images.pathCache")

  def resizeImage(deviceUid: String): Action[AnyContent] = Action.async {
    debugParam
    database.runAsync(
      (for {
        fcRow <- Tables.SmFileCard
        if fcRow.deviceUid === deviceUid && fcRow.fMimeTypeJava === "image/jpeg" && fcRow.sha256.nonEmpty
      }
        yield (fcRow.id, fcRow.fParent, fcRow.fName, fcRow.fExtension, fcRow.sha256)
        ).result)
      .map { rowSeq =>
        FileUtils.getDeviceInfo(deviceUid) map { device =>
          if (device.isDefined) {
            val mountPoint = device.head.mountpoint

            debug(rowSeq.length)

            rowSeq.foreach { cFc =>
              SmImageUtil.saveImageResize(
                pathCache,
                mountPoint + OsConf.fsSeparator + cFc._2 + cFc._3,
                cFc._3,
                cFc._4.getOrElse(""),
                cFc._5.get)
            }
          }
        }
        Ok("run resizeImage")
      }
  }

  def viewImages(deviceUid: String, fParent: String): Action[AnyContent] = Action.async {

    database.runAsync(Tables.SmFileCard
      .filter(_.deviceUid === deviceUid)
      .filter(_.fParent === fParent)
      .filter(_.sha256.nonEmpty)
      .filter(_.fMimeTypeJava === "image/jpeg")
      .sortBy(_.fLastModifiedDate.desc)
      .map(fld => (fld.sha256, fld.fName, fld.fExtension, fld.fMimeTypeJava)).result)
      .map { dbGet =>
        val images: Seq[(String, Option[String], String, Option[String])] =
          dbGet.take(maxResult).map { row =>
            (pathCache + OsConf.fsSeparator + SmImageUtil.getImageKey(row._1.get, row._2, row._3.getOrElse("")),
              row._1, row._2, row._4
            )
          }
        debug(images)
        Ok(views.html.view_image(dbGet.size, maxResult, images))
      }
  }

  def viewImage(fullPath: String, mimeType: Option[String]): Action[AnyContent] = Action.async {
    val file = better.files.File(fullPath)
    val path: java.nio.file.Path = file.path
    val source: Source[ByteString, _] = FileIO.fromPath(path)

    val contentLength = Some(file.size)

    Future.successful(
      Result(
        header = ResponseHeader(OK, Map.empty),
        body = HttpEntity.Streamed(source, contentLength, mimeType)
      )
    )
  }
}

package controllers


import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.http.HttpEntity
import play.api.mvc._
import play.api.{Configuration, Logger}
import ru.ns.model.OsConf
import ru.ns.tools.{FileUtils, SmImageUtil}
import services.db.DBService
import slick.basic.DatabasePublisher
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * Created by ns on 29.10.2019
  */
@Singleton
class SmImage @Inject()(config: Configuration, val database: DBService)
  extends InjectedController {

  val logger: Logger = play.api.Logger(getClass)
  val maxResult: Int = config.get[Int]("Images.maxResult")
  val pathCache: String = config.get[String]("Images.pathCache")

  implicit val system: ActorSystem = ActorSystem()

  type DbImageRes = (String, String, Option[String], Option[String])

  def resizeImage(deviceUid: String): Action[AnyContent] = Action.async {
    debugParam

    FileUtils.getDeviceInfo(deviceUid) map { device =>
      if (device.isDefined) {
        val mountPoint: String = device.head.mountpoint
        val dbFcStream: Source[DbImageRes, NotUsed] = getStreamImageByDevice(deviceUid)
        dbFcStream
          .throttle(elements = 400, 10.millisecond, maximumBurst = 1, mode = ThrottleMode.Shaping)
          .mapAsync(2)(writeImageResizeToDb(_, mountPoint))
          .runWith(Sink.ignore)
      }
    }

    Future.successful(Ok("run resizeImage"))
  }

  def getStreamImageByDevice(deviceUid: String): Source[DbImageRes, NotUsed] = {

    // TODO add group by (fcRow.fParent, fcRow.fName, fcRow.fExtension, fcRow.sha256)
    // TODO add job_resize
    val queryRes = (for {
      fcRow <- Tables.SmFileCard
      if fcRow.deviceUid === deviceUid && fcRow.fMimeTypeJava === "image/jpeg" && fcRow.sha256.nonEmpty && !Tables.SmImageResize
        .filter(imgRes => fcRow.sha256 === imgRes.sha256 && fcRow.fName === imgRes.fName)
        .map(p => p.fName)
        .exists
    }
      yield (fcRow.fParent, fcRow.fName, fcRow.fExtension, fcRow.sha256)
      ).result
    val databasePublisher: DatabasePublisher[DbImageRes] = database runStream queryRes
    val akkaSourceFromSlick: Source[DbImageRes, NotUsed] = Source fromPublisher databasePublisher

    akkaSourceFromSlick
  }

  def writeImageResizeToDb(cFc: (String, String, Option[String], Option[String]), mountPoint: String): Future[Future[(String, String)]] = {
    SmImageUtil.saveImageResize(
      pathCache,
      mountPoint + OsConf.fsSeparator + cFc._1 + cFc._2,
      cFc._2,
      cFc._3.getOrElse(""),
      cFc._4.get).map { file_id =>
      if (file_id.isDefined) {
        val cRow = Tables.SmImageResizeRow(cFc._4.get, cFc._2, file_id.get)
        database.runAsync((Tables.SmImageResize returning Tables.SmImageResize.map(r => (r.sha256, r.fName))) += models.SmImageResize.apply(cRow).data.toRow)
      } else {
        Future.successful(("",""))
      }
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
        Ok(views.html.view_image(dbGet.size, maxResult, images)())
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

package controllers

import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, _}
import play.api.mvc.{Action, _}
import services.db.DBService
import utils.db.SmPostgresDriver
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SmSearch @Inject()(val database: DBService)(implicit assetsFinder: AssetsFinder)
  extends InjectedController {

  val logger: Logger = play.api.Logger(getClass)

  def queryForm: Action[AnyContent] = Action {
    Ok(views.html.sm_search_autocomplete(assetsFinder))
  }

  case class Paging(draw: Int,
                    recordsTotal: Int,
                    recordsFiltered: Int,
                    data: Seq[FilePath],
                    error: String
                   ) {
  }

  case class FilePath(/*id: String,*/
                      name: String,
                      path: String,
                      sha256: String,
                      device: String
                     ) {

  }

  implicit val locationFilePath: Writes[FilePath] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "path").write[String] and
      (JsPath \ "sha256").write[String] and
      (JsPath \ "device").write[String]
    ) (unlift(FilePath.unapply))

  implicit lazy val pagingWrites: Writes[Paging] = (
    (JsPath \ "draw").write[Int] and
      (JsPath \ "recordsTotal").write[Int] and
      (JsPath \ "recordsFiltered").write[Int] and
      (JsPath \ "data").write[Seq[FilePath]] and
      (JsPath \ "error").write[String]
    ) (unlift(Paging.unapply))


  /**
    * https://github.com/abdheshkumar/PlayFrameWork_DataTable/blob/master/app/controllers/Application.scala
    * https://editor.datatables.net/examples/inline-editing/serverSide.html
    *
    * @param draw   draw
    * @param start  start
    * @param length length
    * @return
    */
  def getFilesbyName(draw: Int, start: Int, length: Int): Action[AnyContent] = Action.async { implicit request =>
    val search = request.getQueryString("search[value]").getOrElse("").replace(" ", "%").toLowerCase()
    val sortCol = request.getQueryString("order[0][column]").getOrElse("").toInt
    val sortDir = request.getQueryString("order[0][dir]").getOrElse("")
    debugParam
    logger.debug(s"search = $search   sortCol = $sortCol   sortDir = $sortDir")

    val baseQry = for {
      (fcRow, device) <- Tables.SmFileCard joinLeft Tables.SmDevice on ((fc, device) => {
        fc.deviceUid === device.uid
      })
    } yield (fcRow, device)

    val cntAll = baseQry.groupBy(uRow => (uRow._1.fNameLc, uRow._1.fName, uRow._1.fParent, uRow._1.sha256, uRow._2.map(_.labelV)))
      .map({ case (uRow, cnt) => (uRow, cnt.map(_._1).length) })
      .length.result
    val filtered = baseQry.filter(_._1.fNameLc.like("%" + search + "%"))
      .groupBy(uRow => (uRow._1.fNameLc, uRow._1.fName, uRow._1.fParent, uRow._1.sha256, uRow._2.map(_.labelV)))
      .map({ case (uRow, cnt) => (uRow, cnt.map(_._1).length) })
    val cntFiltered = filtered.length.result
    val qryBySearch = filtered.drop(start).take(length)

    val sortedQry = getSortedQry(sortCol, sortDir, qryBySearch)

    val composedAction = for {cntAll <- cntAll
                              cntFiltered <- cntFiltered
                              qryBySearch <- sortedQry.result
    } yield (cntAll, cntFiltered, qryBySearch)

    database.runAsync(composedAction).map { rowSeq =>
      val filePath = ArrayBuffer[FilePath]()
      rowSeq._3.foreach { p => filePath += FilePath(name = p._1._2, path = p._1._3, sha256 = p._1._4.getOrElse(""), device = p._1._5.getOrElse("")) }

      val ret = Paging(draw, rowSeq._1, rowSeq._2, filePath.toSeq, "")

      Ok(Json.toJson(ret))
    }
  }

  def getSortedQry(sortCol: Int,
                   sortDir: String,
                   qryBySearch: Query[((Rep[String], Rep[String], Rep[String], Rep[Option[String]], Rep[Option[String]]), Rep[Int]), ((String, String, String, Option[String], Option[String]), Int), scala.Seq]
                  ): Query[((SmPostgresDriver.api.Rep[String], SmPostgresDriver.api.Rep[String], SmPostgresDriver.api.Rep[String], SmPostgresDriver.api.Rep[Option[String]],
    SmPostgresDriver.api.Rep[Option[String]]), SmPostgresDriver.api.Rep[Int]), ((String, String, String, Option[String], Option[String]), Int), Seq] = {

    (sortCol, sortDir) match {
      case (0, "desc") => qryBySearch.sortBy(_._1._2.desc)
      case (1, "asc") => qryBySearch.sortBy(_._1._3)
      case (1, "desc") => qryBySearch.sortBy(_._1._3.desc)
      case (2, "asc") => qryBySearch.sortBy(_._1._4)
      case (2, "desc") => qryBySearch.sortBy(_._1._4.desc)
      case (3, "asc") => qryBySearch.sortBy(_._1._5)
      case (3, "desc") => qryBySearch.sortBy(_._1._5.desc)
      case (_, _) => qryBySearch.sortBy(_._1._2)
    }
  }
}

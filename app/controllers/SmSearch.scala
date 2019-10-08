package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, _}
import play.api.mvc.{Action, _}
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SmSearch @Inject()(val database: DBService)(implicit assetsFinder: AssetsFinder)
  extends InjectedController {

  val logger = play.api.Logger(getClass)
  val gLimit: Int = 100

  def queryForm: Action[AnyContent] = Action {
    Ok(views.html.sm_search_autocomplete(assetsFinder))
  }

  def byFileName(fileName: String, limit: Int): Action[AnyContent] = Action.async {
    val maxLimit: Int = Math.min(limit, gLimit)
    val fileNameFnd = fileName.replace(" ", "%").toLowerCase()

    logger.debug(s"fileName - $fileName")

    val qry = sql"""
       SELECT DISTINCT fc.f_name
       FROM sm_file_card fc
       WHERE lower(fc.f_name_lc) LIKE '%#$fileNameFnd%'
       order by fc.f_name
       limit '#$maxLimit'
      """
      .as[String]

    database.runAsync(qry).map { rowSeq =>
      logger.debug(rowSeq.size.toString)

      Ok(Json.toJson(rowSeq))
    }
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
                      sha256: String
                     ) {

  }

  implicit val locationFilePath: Writes[FilePath] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "path").write[String] and
      (JsPath \ "sha256").write[String]
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
    val where = sql"""WHERE fc.f_name_lc LIKE '%#$search%'"""

    debugParam
    logger.debug(s"search = $search")
    logger.debug(where.toString)

    val cntAll = sql"""     SELECT COUNT(*) FROM (SELECT DISTINCT fc.f_name, fc.f_parent, fc.sha256 FROM sm_file_card fc) res""".as[(Int)]
    val cntFiltered = sql"""SELECT COUNT(*) FROM (SELECT DISTINCT fc.f_name, fc.f_parent, fc.sha256 FROM sm_file_card fc WHERE fc.f_name_lc LIKE '%#$search%' ) res""".as[(Int)]

    val qry = sql"""SELECT DISTINCT fc.f_name, fc.f_parent, fc.sha256 FROM sm_file_card fc
       WHERE fc.f_name_lc LIKE '%#$search%'
       ORDER BY fc.f_name offset '#$start' limit '#$length'
      """.as[(String, String, String)]

    val composedAction = for {cntAll <- cntAll
                              cntFiltered <- cntFiltered
                              qry <- qry} yield (cntAll, cntFiltered, qry)

    database.runAsync(composedAction).map { rowSeq =>
      val filePath = ArrayBuffer[FilePath]()
      rowSeq._3.foreach { p => filePath += FilePath(name = p._1, path = p._2, sha256 = p._3) }

      val ret = Paging(draw, rowSeq._1.head, rowSeq._2.head, filePath.toSeq, "")

      //      val qwe = Json.toJson(ret)
      //      println(Json.prettyPrint(qwe))
      //      println(qwe)

      Ok(Json.toJson(ret))
    }
  }
}

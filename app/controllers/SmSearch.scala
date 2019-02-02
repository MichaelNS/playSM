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

  val gLimit: Int = 100

  def queryForm: Action[AnyContent] = Action {
    Ok(views.html.sm_search_autocomplete(assetsFinder))
  }

  def byFileName(fileName: String, limit: Int): Action[AnyContent] = Action.async {
    val maxLimit: Int = Math.min(limit, gLimit)
    val fileNameFnd = fileName.replace(" ", "%")

    val qry = sql"""
       SELECT DISTINCT fc."F_NAME"
       FROM "sm_file_card" fc
       WHERE lower(fc."F_NAME_LC") LIKE '%#$fileNameFnd%'
       order by fc."F_NAME"
       limit '#$maxLimit'
      """
      .as[String]

    database.runAsync(qry).map { rowSeq =>
      Ok(Json.toJson(rowSeq))
    }
  }

  case class FilePath(name: String,
                      path: String,
                      sha256: String
                     ) {

  }

  implicit val locationFilePath: Writes[FilePath] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "path").write[String] and
      (JsPath \ "sha256").write[String]
    ) (unlift(FilePath.unapply))

  def getFilesbyName(fileName: String, limit: Int): Action[AnyContent] = Action.async {
    val maxLimit: Int = Math.min(limit, gLimit)

    val qry = sql"""
       SELECT DISTINCT fc."F_NAME", fc."F_PARENT", fc."SHA256"
       FROM "sm_file_card" fc
       WHERE fc."F_NAME" = '#$fileName'
       order by fc."F_NAME"
       limit '#$maxLimit'
      """
      .as[(String, String, String)]

    database.runAsync(qry).map { rowSeq =>
      val filePath = ArrayBuffer[FilePath]()
      rowSeq.foreach { p => filePath += FilePath(name = p._1, path = p._2, sha256 = p._3) }

      Ok(Json.toJson(filePath))
    }
  }
}

package controllers.browser

import java.time.LocalDateTime

import controllers.getDateTimeResult
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SmExifDevice @Inject()(cc: MessagesControllerComponents, val database: DBService)
  extends MessagesAbstractController(cc) {


  def listExifDevices: Action[AnyContent] = Action.async {
    val qry = sql"""
        SELECT ex.make,
               ex.model,
               max(to_char(f_last_modified_date, 'YYYY-MM-DD HH24:MI:SS')),
               count(1)
        FROM sm_file_card
                 inner join sm_exif ex on sm_file_card.id = ex.id
        GROUP BY ex.make, ex.model
        order by 3 desc
      """
      .as[(String, String, LocalDateTime, Int)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.browser.sm_exif_device(rowSeq)())
    }
  }

  def listFilesByExifDevice(exifMake: String, exifDevice: String): Action[AnyContent] = Action.async {
    val qry = sql"""
        SELECT fc.SHA256,
               fc.f_name,
               array_agg(DISTINCT fc.f_parent),
               array_agg(DISTINCT dv.label_v),
               to_char(f_last_modified_date, 'YYYY-MM-DD HH24:MI:SS')
        FROM sm_file_card fc
                 inner join sm_device dv on dv.uid = fc.device_uid
                 inner join sm_exif ex on fc.id = ex.id
        where ex.make = '#$exifMake'
          and ex.model = '#$exifDevice'
        group by fc.SHA256, fc.f_name, fc.f_parent, dv.label_v, f_last_modified_date
        order by 5 desc
      """
      .as[(String, String, String, String, LocalDateTime)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.browser.sm_files_by_device(rowSeq)())
    }
  }

}

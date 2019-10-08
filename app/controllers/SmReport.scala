package controllers

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.SmFileCard
import models.db.Tables
import org.joda.time.DateTime
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import ru.ns.model.OsConf
import services.db.DBService
import slick.jdbc.GetResult
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

/**
  * Created by ns on 02.03.2017.
  */
@Singleton
class SmReport @Inject()(cc: MessagesControllerComponents, val database: DBService)
  extends MessagesAbstractController(cc) {

  def listFilesWithoutSha256ByDevice(device: String): Action[AnyContent] = Action.async {
    val maxRows = 200
    val baseQry = Tables.SmFileCard
      .filter(fc => fc.storeName === device && fc.sha256.isEmpty && fc.fSize > 0L)
      .sortBy(_.fLastModifiedDate.desc)
      .map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate))

    val composedAction = for {cnt <- baseQry.length.result
                              qry <- baseQry.take(maxRows).result} yield (cnt, qry)

    database.runAsync(composedAction).map { rowSeq =>
      Ok(views.html.filecards(Some(rowSeq._1), Some(maxRows), rowSeq._2)
      )
    }
  }

  def checkBackUp(device: String): Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val backUpVolumes: String = config.getStringList("BackUp.volumes").asScala.toSet.mkString("'", "', '", "'")
    val maxRows: Long = config.getLong("BackUp.maxResult")

    implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

    val qry = sql"""
      SELECT f_parent, f_name, f_last_modified_date FROM sm_file_card fc WHERE store_name = '#$device'
      AND   sha256 IS NOT NULL
      AND   NOT EXISTS (SELECT 1
                        FROM sm_file_card
                        WHERE sha256 IS NOT NULL
                        AND   fc.sha256 = sha256
                        AND   store_name != '#$device'
                        AND   store_name IN (#$backUpVolumes))
                        AND   NOT f_parent LIKE '%^_files' ESCAPE '^'
      AND   NOT f_parent LIKE '%^_files' escape '^'
      ORDER BY f_parent, f_name LIMIT #$maxRows""".as[(String, String, DateTime)]

    val cnt = sql"""
      SELECT count(1) FROM sm_file_card fc WHERE store_name = '#$device'
      AND   sha256 IS NOT NULL
      AND   NOT EXISTS (SELECT 1
                        FROM sm_file_card
                        WHERE sha256 IS NOT NULL
                        AND   fc.sha256 = sha256
                        AND   store_name != '#$device'
                        AND   store_name IN (#$backUpVolumes))
                        AND   NOT f_parent LIKE '%^_files' ESCAPE '^'
      AND   NOT f_parent LIKE '%^_files' escape '^'
      """.as[Int]

    val composedAction = for {cnt <- cnt
                              qry <- qry} yield (cnt, qry)

    database.runAsync(composedAction).map { rowSeq =>
      Ok(views.html.sm_chk_device_backup(rowSeq._1, maxRows, rowSeq._2))
    }
  }

  def checkBackAllFiles: Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val cntFiles: Int = config.getInt("BackUp.allFiles.cntFiles")
    val maxRows: Int = config.getInt("BackUp.allFiles.maxRows")
    val device_Unreliable: String = config.getStringList("BackUp.allFiles.device_Unreliable").asScala.toSet.mkString("'", "', '", "'")
    val device_NotView: String = config.getStringList("BackUp.allFiles.device_NotView").asScala.toSet.mkString("'", "', '", "'")

    debug(device_Unreliable)
    debug(device_NotView)

    val qry = sql"""
       SELECT
         sha256,
         f_name,
         category_type,
         description,
         store_name
       FROM (
              SELECT
                card.sha256,
                card.f_name,
                category.category_type,
                category.description,
                (SELECT store_name
                 FROM sm_file_card sq
                 WHERE sq.sha256 = card.sha256
                 AND   sq.store_name NOT IN (#$device_Unreliable)
                 LIMIT 1) AS store_name
              FROM "sm_file_card" card
                JOIN sm_category_fc category ON category.f_name = card.f_name and category.id = card.sha256
              WHERE category.category_type IS NOT NULL
              GROUP BY card.sha256,
                       card.f_name,
                       category.category_type,
                       category.description
              HAVING COUNT(1) < #$cntFiles
            ) AS res
       WHERE store_name NOT IN (#$device_NotView)
       LIMIT #$maxRows
      """
      .as[(String, String, String, String, String)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.sm_chk_all_backup(rowSeq, device_Unreliable, device_NotView, cntFiles, rowSeq.length, maxRows))
    }

  }

  def checkBackFilesLastYear: Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val cntFiles: Int = config.getInt("BackUp.allFiles.cntFiles")
    val maxRows: Int = config.getInt("BackUp.allFiles.maxRows")
    val device_Unreliable: String = config.getStringList("BackUp.allFiles.device_Unreliable").asScala.toSet.mkString("'", "', '", "'")
    val device_NotView: String = config.getStringList("BackUp.allFiles.device_NotView").asScala.toSet.mkString("'", "', '", "'")

    implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

    debug(device_Unreliable)
    debug(device_NotView)

    val qry = sql"""
       SELECT
         sha256,
         f_name,
         store_name,
         f_last_modified_date
       FROM (
              SELECT
                card.sha256,
                card.f_name,
                (SELECT store_name
                 FROM sm_file_card sq
                 WHERE sq.sha256 = card.sha256
                 AND   sq.store_name NOT IN (#$device_Unreliable)
                 LIMIT 1) AS store_name,
                 card.f_last_modified_date
              FROM sm_file_card card
              WHERE card.f_last_modified_date >= date_trunc('month', card.f_last_modified_date) - INTERVAL '1 year'
              GROUP BY card.sha256,
                       card.f_name,
                       card.f_last_modified_date
              HAVING COUNT(1) < #$cntFiles
              order by card.f_last_modified_date desc
            ) AS res
       WHERE store_name NOT IN (#$device_NotView)
       LIMIT #$maxRows
      """
      .as[(String, String, String, DateTime)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.sm_chk_backup_last_year(rowSeq, device_Unreliable, device_NotView, cntFiles, rowSeq.length, maxRows))
    }
  }

  /**
    * check duplicates SmFileCard
    * Call from [[views.html.smd_index]]
    *
    * @param device device uid
    * @return [[views.html.f_duplicates]]
    */
  def checkDuplicates(device: String): Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxFileSize: Long = config.getBytes("checkDuplicates.maxFileSize")

    val qry = (for {
      uRow <- Tables.SmFileCard if uRow.storeName === device && uRow.fSize > 0L && uRow.fSize > maxFileSize && uRow.sha256.nonEmpty
      v_fName <- Tables.SmFileCard if v_fName.id === uRow.id
    } yield (uRow, v_fName))
      .groupBy({
        case (uRow, v_fName) =>
          (uRow.sha256, v_fName.fName, v_fName.fSize)
      })
      .map({
        case ((uRow, v_fName, b_fName), cnt) =>
          (uRow, v_fName, b_fName, cnt.map(_._1.sha256).length)
      })
      .filter(cnt => cnt._4 > 1)
      .sortBy(r => (r._4.desc, r._3.desc))

    database.runAsync(
      qry.result
    ).map { rowSeq =>

      Ok(views.html.f_duplicates(device, maxFileSize, rowSeq))
    }
  }

  /**
    * Call from [[SmReport.checkDuplicates]] -> [[views.html.f_duplicates]]
    *
    * @param device device uid
    * @param sha256 sha256
    * @return [[views.html.sm_device_sha256]]
    */
  def getFcByDeviceSha256(device: String, sha256: String): Action[AnyContent] = Action.async {

    val qry = for {
      (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.id && fc.fName === cat.fName
      }) if fcRow.storeName === device && fcRow.sha256 === sha256
    } yield (fcRow.id, fcRow.fName, fcRow.fParent, fcRow.fLastModifiedDate, catRow.map(_.categoryType), catRow.map(_.description))

    database.runAsync(
      qry
        .sortBy(_._3)
        .result
    ).map { rowSeq =>
      Ok(views.html.sm_device_sha256(device, rowSeq))
    }
  }

  /**
    * Explorer device
    *
    * @param device   device
    * @param treePath treePath
    * @param cPath    path
    * @param depth    path depth
    * @return
    */
  def explorerDevice(device: String, treePath: String, cPath: String, depth: Int): Action[AnyContent] = Action.async {
    debugParam

    val qry = sql"""
      SELECT
        split_part(x2.f_parent, '/', #$depth),
        count(1),
        count(1) filter (where sm_category_fc is null),
        array_agg(DISTINCT sm_category_fc.category_type) filter (where sm_category_fc is not null)
    FROM sm_file_card x2
           left outer join sm_category_fc on x2.sha256 = sm_category_fc.id
    WHERE (((x2.store_name = '#$device')))
      AND (NOT (x2.f_parent LIKE '%^_files' ESCAPE '^'))
      AND (NOT (x2.f_parent LIKE '%^_files/' ESCAPE '^'))
      AND split_part(x2.f_parent, '/', #$depth -1) = '#$cPath'
      GROUP BY split_part(x2.f_parent, '/', #$depth)
      ORDER BY split_part(x2.f_parent, '/', #$depth)
      """
      .as[(String, Int, Int, String)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.fc_explorer(device, treePath, rowSeq, depth))
    }
  }

  /**
    * Show dirs without SmCategoryFc and files HAVING count(1) > maxFiles
    * Call from [[SmApplication.listStoreNameAndCnt]] -> [[views.html.storename]]
    *
    * @param device   device uid
    * @param maxFiles maximum files - group by F_PARENT having count(1) > maxFiles
    * @return [[views.html.dirs_fc]]
    */
  def lstDirByDevice(device: String, maxFiles: Int): Action[AnyContent] = Action.async {
    val qry = for {
      (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.id && fc.fName === cat.fName
      }) if fcRow.storeName === device && fcRow.sha256.nonEmpty && catRow.isEmpty
    } yield fcRow
    debugParam

    database.runAsync(
      qry
        .filterNot(_.fParent endsWith "_files")
        .filterNot(_.fParent endsWith "_files" + OsConf.fsSeparator)
        .groupBy(_.fParent)
        .map({
          case (fld, cnt) => (fld, cnt.map(_.fParent).length)
        })
        .filter { case (_, cnt) => cnt > maxFiles }
        .sortBy(_._1)
        .result)
      .map { rowSeq =>
        //        val max = 20
        //        rowSeq.take(max) foreach { q => Logger.debug(s"${q._1}   ${q._1.count(_ == '\\')}") }
        //        Ok("123")
        Ok(views.html.dirs_fc(rowSeq))
      }
  }
}

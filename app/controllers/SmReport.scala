package controllers

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.SmFileCard
import models.db.Tables
import play.api.mvc.{Action, AnyContent, InjectedController}
import ru.ns.model.OsConf
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ns on 02.03.2017.
  */
@Singleton
class SmReport @Inject()(val database: DBService)
  extends InjectedController {

  def cntFilesWithoutSha256ByDevice(device: String): Action[AnyContent] = Action.async {
    database.runAsync(
      Tables.SmFileCard
        .filter(_.storeName === device)
        .filter(_.sha256.isEmpty)
        .filter(_.fSize > 0L)
        .length
        .result)
      .map { rowSeq =>
        Ok(rowSeq.toString)
      }
  }

  def listFilesWithoutSha256ByDevice(device: String): Action[AnyContent] = Action.async {
    database.runAsync(
      Tables.SmFileCard
        .filter(_.storeName === device)
        .filter(_.sha256.isEmpty)
        .filter(_.fSize > 0L)
        .sortBy(_.fLastModifiedDate.desc)
        .take(200)
        .result)
      .map { rowSeq =>
        val smFcs = rowSeq.map(SmFileCard(_))
        Ok(views.html.filecards(smFcs))
      }
  }

  def checkBackUp(device: String): Action[AnyContent] = Action.async {
    import scala.collection.JavaConverters._
    val config = ConfigFactory.load("scanImport.conf")
    val backUpVolumes: Set[String] = config.getStringList("BackUp.volumes").asScala.toSet
    val maxResult: Long = config.getLong("BackUp.maxResult")

    val lstBackUpSha256 = Tables.SmFileCard
      .filter(_.sha256.nonEmpty)
      .filter(_.storeName =!= device)
      .filterNot(_.fParent endsWith "_files")
      .filter(_.storeName inSet backUpVolumes)
      .map(_.sha256).distinct

    database.runAsync(
      Tables.SmFileCard
        .filter(_.storeName === device)
        .filter(_.sha256.nonEmpty)
        .filterNot(_.fParent endsWith "_files")
        .filterNot(_.sha256 in lstBackUpSha256)
        .sortBy(_.fName)
        .sortBy(_.fParent)
        .take(maxResult)
        .result)
      .map { rowSeq =>
        val smFcs = rowSeq.map(SmFileCard(_))
        Ok(views.html.filecards(smFcs))
      }
  }

  def checkBackAllFiles: Action[AnyContent] = Action.async {
    import scala.collection.JavaConverters._
    val config = ConfigFactory.load("scanImport.conf")
    val cntFiles: Int = config.getInt("BackUp.allFiles.cntFiles")
    val maxRows: Int = config.getInt("BackUp.allFiles.maxRows")
    val device_Unreliable: String = config.getStringList("BackUp.allFiles.device_Unreliable").asScala.toSet.mkString("'", "', '", "'")
    val device_NotView: String = config.getStringList("BackUp.allFiles.device_NotView").asScala.toSet.mkString("'", "', '", "'")

    debug(device_Unreliable)
    debug(device_NotView)

    val qry = sql"""
       SELECT
         "SHA256",
         "F_NAME",
         "CATEGORY_TYPE",
         "DESCRIPTION",
         "STORE_NAME"
       FROM (
              SELECT
                card."SHA256",
                card."F_NAME",
                category."CATEGORY_TYPE",
                category."DESCRIPTION",
                (SELECT "STORE_NAME"
                 FROM sm_file_card sq
                 WHERE sq."SHA256" = card."SHA256"
                 AND   sq."STORE_NAME" NOT IN (#$device_Unreliable)
                 LIMIT 1) AS "STORE_NAME"
              FROM "sm_file_card" card
                JOIN sm_category_fc category ON category."F_NAME" = card."F_NAME" and category."ID" = card."SHA256"
              WHERE category."CATEGORY_TYPE" IS NOT NULL
              GROUP BY card."SHA256",
                       card."F_NAME",
                       category."CATEGORY_TYPE",
                       category."DESCRIPTION"
              HAVING COUNT(1) < #$cntFiles
            ) AS res
       WHERE "STORE_NAME" NOT IN (#$device_NotView)
       LIMIT #$maxRows
      """
      .as[(String, String, String, String, String)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.sm_chk_all_backup(rowSeq, device_Unreliable, device_NotView, cntFiles, rowSeq.length, maxRows))
    }

  }

  def checkBackFilesLastYear: Action[AnyContent] = Action.async {
    import scala.collection.JavaConverters._
    val config = ConfigFactory.load("scanImport.conf")
    val cntFiles: Int = config.getInt("BackUp.allFiles.cntFiles")
    val maxRows: Int = config.getInt("BackUp.allFiles.maxRows")
    val device_Unreliable: String = config.getStringList("BackUp.allFiles.device_Unreliable").asScala.toSet.mkString("'", "', '", "'")
    val device_NotView: String = config.getStringList("BackUp.allFiles.device_NotView").asScala.toSet.mkString("'", "', '", "'")

    debug(device_Unreliable)
    debug(device_NotView)

    val qry = sql"""
       SELECT
         "SHA256",
         "F_NAME",
         "STORE_NAME"
       FROM (
              SELECT
                card."SHA256",
                card."F_NAME",
                (SELECT "STORE_NAME"
                 FROM sm_file_card sq
                 WHERE sq."SHA256" = card."SHA256"
                 AND   sq."STORE_NAME" NOT IN (#$device_Unreliable)
                 LIMIT 1) AS "STORE_NAME"
              FROM "sm_file_card" card
              WHERE card."F_LAST_MODIFIED_DATE" >= date_trunc('month', card."F_LAST_MODIFIED_DATE") - INTERVAL '1 year'
              GROUP BY card."SHA256",
                       card."F_NAME",
                       card."F_LAST_MODIFIED_DATE"
              HAVING COUNT(1) < #$cntFiles
              order by card."F_LAST_MODIFIED_DATE" desc
            ) AS res
       WHERE "STORE_NAME" NOT IN (#$device_NotView)
       LIMIT #$maxRows
      """
      .as[(String, String, String)]
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
    * Call from [[SmReport.checkDuplicates()]] -> [[views.html.f_duplicates]]
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
    * @param device device
    * @param path   path
    * @param depth  path depth
    * @return
    */
  def explorerDevice(device: String, path: String, depth: Int): Action[AnyContent] = Action.async {
    debugParam

    val qry = sql"""
      SELECT
        split_part(x2."F_PARENT", '/', #$depth),
        count(1),
        count(1) filter (where sm_category_fc is null),
        array_agg(DISTINCT sm_category_fc."CATEGORY_TYPE") filter (where sm_category_fc is not null)
    FROM "sm_file_card" x2
           left outer join sm_category_fc on x2."SHA256" = sm_category_fc."ID"
    WHERE (((x2."STORE_NAME" = '#$device')))
      AND (NOT (x2."F_PARENT" LIKE '%^_files' ESCAPE '^'))
      AND (NOT (x2."F_PARENT" LIKE '%^_files/' ESCAPE '^'))
      AND split_part(x2."F_PARENT", '/', #$depth -1) = '#$path'
      GROUP BY split_part(x2."F_PARENT", '/', #$depth)
      ORDER BY split_part(x2."F_PARENT", '/', #$depth)
      """
      .as[(String, Int, Int, String)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.fc_explorer(device, rowSeq, depth))
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

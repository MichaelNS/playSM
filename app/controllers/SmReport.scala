package controllers

import java.time.LocalDateTime

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import ru.ns.model.OsConf
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
  * Created by ns on 02.03.2017.
  */
@Singleton
class SmReport @Inject()(cc: MessagesControllerComponents, config: Configuration, val database: DBService)
  extends MessagesAbstractController(cc)
    with play.api.i18n.I18nSupport {

  def listFilesWithoutSha256ByDevice(device: String): Action[AnyContent] = Action.async {
    val maxRows = 200
    val baseQry = Tables.SmFileCard
      .filter(fc => fc.deviceUid === device && fc.sha256.isEmpty && fc.fSize > 0L)
      .sortBy(_.fLastModifiedDate.desc)
      .map(fld => (fld.fParent, fld.fName, fld.fLastModifiedDate))

    val composedAction = for {cnt <- baseQry.length.result
                              qry <- baseQry.take(maxRows).result} yield (cnt, qry)

    database.runAsync(composedAction).map { rowSeq =>
      Ok(views.html.filecards(Some(rowSeq._1), Some(maxRows), rowSeq._2)()
      )
    }
  }

  /**
    * https://stackoverflow.com/questions/32262353/how-to-do-a-correlated-subquery-in-slick
    * for {
    * a <- A if !B.filter(b => b.fieldK === a.fieldA).exists
    * } yield (a.fieldA)
    *
    * @param device device
    * @return
    */
  def checkBackUp(device: String): Action[AnyContent] = Action.async {
    val backUpVolumes = config.get[Seq[String]]("BackUp.volumes")
    val maxRows: Long = config.get[Long]("BackUp.maxResult")

    val baseQry = for {
      a <- Tables.SmFileCard
      if a.sha256.nonEmpty && a.deviceUid === device && !Tables.SmFileCard
        .filter(b => b.sha256.nonEmpty && b.sha256 === a.sha256 && b.deviceUid =!= device && b.deviceUid.inSet(backUpVolumes))
        .filterNot(b => b.fParent endsWith "_files")
        .map(p => p.fName)
        .exists
    } yield (a.fParent, a.fName, a.fLastModifiedDate)

    val cnt = baseQry.length
    val filtQry = baseQry
      .sortBy(r => (r._1, r._2))
      .take(maxRows)

    val composedAction = for {cnt <- cnt.result
                              filtQry <- filtQry.result} yield (cnt, filtQry)

    database.runAsync(composedAction).map { rowSeq =>
      Ok(views.html.sm_chk_device_backup(rowSeq._1, maxRows, rowSeq._2)())
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
         device_uid
       FROM (
              SELECT
                card.sha256,
                card.f_name,
                category_rule.category_type,
                category_rule.description,
                (SELECT device_uid
                 FROM sm_file_card sq
                 WHERE sq.sha256 = card.sha256
                 AND   sq.device_uid NOT IN (#$device_Unreliable)
                 LIMIT 1) AS device_uid
              FROM "sm_file_card" card
                JOIN sm_category_fc category ON category.f_name = card.f_name and category.sha256 = card.sha256
                JOIN sm_category_rule category_rule ON category_rule.id = category.id
              WHERE category_rule.category_type IS NOT NULL
              GROUP BY card.sha256,
                       card.f_name,
                       category_rule.category_type,
                       category_rule.description
              HAVING COUNT(1) < #$cntFiles
            ) AS res
       WHERE device_uid NOT IN (#$device_NotView)
       LIMIT #$maxRows
      """
      .as[(String, String, String, String, String)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.sm_chk_all_backup(rowSeq, device_Unreliable, device_NotView, cntFiles, rowSeq.length, maxRows)())
    }

  }

  def checkBackFilesLastYear: Action[AnyContent] = Action.async {
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
         device_uid,
         f_last_modified_date
       FROM (
              SELECT
                card.sha256,
                card.f_name,
                (SELECT device_uid
                 FROM sm_file_card sq
                 WHERE sq.sha256 = card.sha256
                 AND   sq.device_uid NOT IN (#$device_Unreliable)
                 LIMIT 1) AS device_uid,
                 card.f_last_modified_date
              FROM sm_file_card card
              WHERE card.f_last_modified_date >= date_trunc('month', card.f_last_modified_date) - INTERVAL '1 year'
              GROUP BY card.sha256,
                       card.f_name,
                       card.f_last_modified_date
              HAVING COUNT(1) < #$cntFiles
              order by card.f_last_modified_date desc
            ) AS res
       WHERE device_uid NOT IN (#$device_NotView)
       LIMIT #$maxRows
      """
      .as[(String, String, String, LocalDateTime)]
    database.runAsync(qry).map { rowSeq =>
      Ok(views.html.sm_chk_backup_last_year(rowSeq, device_Unreliable, device_NotView, cntFiles, rowSeq.length, maxRows)())
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
    val res = checkDuplicatesEx(device, fParent = None, fExtension = None)
    res._1.map { rowSeq =>
      Ok(views.html.f_duplicates(device, res._2, rowSeq)())
    }
  }

  def checkDuplicatesByParent(device: String, fParent: String): Action[AnyContent] = Action.async { implicit request =>
    val formData: ExtensionForm = ExtensionForm.form.bindFromRequest().get
    val extension = if (formData.extension.nonEmpty) Some(formData.extension.toLowerCase) else None

    val res = checkDuplicatesEx(device, fParent = Some(fParent), extension)
    res._1.map { rowSeq =>
      Ok(views.html.f_duplicates(device, res._2, rowSeq)())
    }
  }

  def checkDuplicatesEx(device: String, fParent: Option[String], fExtension: Option[String]): (Future[Seq[(Option[String], String, Option[Long], Int)]], Long) = {
    val config = ConfigFactory.load("scanImport.conf")
    val maxFileSize: Long = config.getBytes("checkDuplicates.maxFileSize")

    val qry = (for {
      uRow <- Tables.SmFileCard if uRow.deviceUid === device && uRow.fSize > 0L && uRow.fSize > maxFileSize && uRow.sha256.nonEmpty
    } yield uRow)
      .filterOpt(fParent)(_.fParent startsWith _)
      .filterOpt(fExtension)(_.fExtension === _)
      .groupBy(uRow =>
        (uRow.sha256, uRow.fName, uRow.fSize))
      .map({
        case ((uRow, fName, fSize), cnt) =>
          (uRow, fName, fSize, cnt.map(_.sha256).length)
      })
      .filter(cnt => cnt._4 > 1)
      .sortBy(r => (r._4.desc, r._3.desc))

    (database.runAsync(qry.result), maxFileSize)
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
      ((fcRow, catRow), rulesRow) <- Tables.SmFileCard.joinLeft(Tables.SmCategoryFc).on((fc, cat) => {
        fc.sha256 === cat.sha256 && fc.fName === cat.fName
      }).joinLeft(Tables.SmCategoryRule).on(_._2.map(_.id) === _.id)

      if fcRow.deviceUid === device && fcRow.sha256 === sha256
    } yield (fcRow.id, fcRow.fName, fcRow.fParent, fcRow.fLastModifiedDate, rulesRow.map(_.categoryType), rulesRow.map(_.description))

    database.runAsync(
      qry
        .sortBy(_._3)
        .result
    ).map { rowSeq =>
      Ok(views.html.sm_device_sha256(device, rowSeq)())
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
        fc.sha256 === cat.sha256 && fc.fName === cat.fName
      }) if fcRow.deviceUid === device && fcRow.sha256.nonEmpty && catRow.isEmpty
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
        Ok(views.html.dirs_fc(rowSeq)())
      }
  }

  /**
    * Check backup android
    * ls -at > /tmp/123.txt
    * <p>
    * Android Debug Bridge
    * <p>
    * adb shell ls /sdcard/DCIM/Camera/ > /tmp/111222
    * <p>
    * /cmpBackupAndroindDeviceByFile/111222
    * <p>
    * <p>
    * chmod 777 /tmp/111222.sh
    * <p>
    * /tmp/111222.sh
    * <p>
    *
    * @param fileName file name
    * @return
    */
  def cmpBackupAndroidDeviceByFile(fileName: String): Action[AnyContent] = Action.async {
    val copyFrom = "/sdcard/DCIM/Camera/"
    val copyTo = "/tmp/cp_back/"

    val file = better.files.File(s"/tmp/$fileName")
    val content: String = file.contentAsString
    val lines = content.split("\n")

    val fileNamesImp = ArrayBuffer[String]()
    val fileNamesExp = ArrayBuffer[String]()

    lines.foreach { tt =>
      val fileName = tt.replace("\n", "").replace("\r", "")
      //      fileNamesImp.append(fileName + "1")
      fileNamesImp.append(fileName)
    }

    database.runAsync(Tables.SmFileCard.filter(_.fName inSet fileNamesImp).map(_.fName).result).map { rowSeq =>
      val files2copy = fileNamesImp diff rowSeq
      files2copy.foreach { fileName =>
        if (fileName.startsWith("IMG_") || fileName.startsWith("VID_")) {
          val sss = s"adb pull $copyFrom$fileName $copyTo$fileName"
          fileNamesExp.append(sss)
        }
      }
      val fileWr = better.files.File(s"/tmp/$fileName.sh")
      fileWr.overwrite(fileNamesExp.mkString("\n"))

      Ok(fileNamesExp.length.toString)
    }
  }

}

package controllers

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.DirWithoutCat
import models.db.Tables
import play.api.mvc._
import services.db.DBService
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ns on 09.11.2019
  */

@Singleton
class SmCategoryView @Inject()(cc: MessagesControllerComponents, val database: DBService)
  extends MessagesAbstractController(cc)
    with play.api.i18n.I18nSupport {

  val logger = play.api.Logger(getClass)

  /**
    * listCategoryAndCnt
    *
    * @return [[views.html.smr_category]]
    */
  def listCategoryAndCnt: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    database.runAsync(
      (for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        })} yield (fcRow, catRow.map(_.categoryType))
        )
        .groupBy(p => p._2)
        .map { case (categoryType, cnt) => (categoryType, cnt.map(_._2).length) }
        .sortBy(_._2.desc)
        .result)
      .map { rowSeq =>
        Ok(views.html.smr_category(rowSeq, ExtensionForm.form))
      }
  }

  /**
    * listSubCategoryAndCnt
    *
    * @param categoryType categoryType
    * @return [[views.html.smr_sub_category]]
    */
  def listSubCategoryAndCnt(categoryType: String): Action[AnyContent] = Action.async {
    database.runAsync(
      (for {
        (fcRow, catRow) <- Tables.SmFileCard join Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        })} yield catRow
        )
        .filter(_.categoryType === categoryType)
        .groupBy(p => p.subCategoryType)
        .map { case (subcategoryType, cnt) => (subcategoryType, cnt.map(_.subCategoryType).length) }
        .sortBy(_._2.desc)
        .result)
      .map { rowSeq =>
        Ok(views.html.smr_sub_category(categoryType, rowSeq))
      }
  }

  /**
    * listDescriptionAndCnt
    *
    * @param categoryType    categoryType
    * @param subCategoryType subCategoryType
    * @return [[views.html.smr_description]]
    */
  def listDescriptionAndCnt(categoryType: String, subCategoryType: String): Action[AnyContent] = Action.async {
    database.runAsync(
      (for {
        (fcRow, catRow) <- Tables.SmFileCard join Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        })} yield catRow
        )
        .filter(_.categoryType === categoryType)
        .filter(_.subCategoryType === subCategoryType)
        .groupBy(p => p.description)
        .map { case (description, cnt) => (description, cnt.map(_.description).length) }
        .sortBy(_._2.desc)
        .result)
      .map { rowSeq =>
        Ok(views.html.smr_description(categoryType, rowSeq))
      }
  }

  /**
    * get Dirs without category, order by lastdate
    *
    * @return [[views.html.cat_list_fc]]
    */
  def listDirWithoutCatByLastDate: Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxFilesTake: Long = config.getBytes("Category.maxFilesTake")
    val qry = (for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
      fc.sha256 === cat.id && fc.fName === cat.fName
    }) if catRow.isEmpty && fcRow.fSize > 0L
                    } yield (fcRow.sha256, fcRow.fParent, fcRow.fLastModifiedDate)
      ).groupBy { p => (p._2, p._3) }
      .map(fld => (fld._1._1, fld._1._2))
    database.runAsync(qry.filterNot(_._1 endsWith "_files")
      .sortBy(_._2.desc)
      .take(maxFilesTake)
      .result
    ).map { rowSeq =>
      val vPath = new mutable.HashMap[String, DirWithoutCat]()
      rowSeq.foreach { row =>
        if (vPath.get(row._1).isEmpty) {
          vPath += (row._1 -> DirWithoutCat(row._1, row._2.toLocalDate))
        }
      }
      var vView = Vector[DirWithoutCat]()
      vPath.toList.sortWith((x, y) => x._2.date.isAfter(y._2.date)).foreach(w =>
        vView = vView :+ w._2
      )
      vPath.clear()

      Ok(views.html.cat_list_path(vView))
    }
  }

  /**
    * get SmFileCard without category, order by lastdate
    *
    * @return [[views.html.cat_list_fc]]
    */
  def listFcWithoutCatByLastDate: Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxFilesTake: Long = config.getBytes("Category.maxFilesTake")

    val qry = (
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        }) if catRow.isEmpty && fcRow.fSize > 0L
      } yield (fcRow.sha256, fcRow.fParent, fcRow.fName, fcRow.fLastModifiedDate)
      )
      .groupBy { p => (p._1, p._2, p._3, p._4) }
      .map(fld => (fld._1._1, fld._1._2, fld._1._3, fld._1._4))

    database.runAsync(
      qry
        .filterNot(_._2 endsWith "_files")
        .sortBy(_._4.desc)
        .take(maxFilesTake)
        .result
    ).map { rowSeq =>
      Ok(views.html.cat_list_fc(rowSeq))
    }
  }



  /**
    * get Dirs without category by extension, order count files
    *
    * @return [[views.html.smr_category_dir_by_ext]]
    */
  def listDirWithoutCategoryByExtension(): Action[AnyContent] = Action.async { implicit request =>
    val config = ConfigFactory.load("scanImport.conf")
    val maxFilesTake: Long = config.getBytes("Category.maxFilesTake")

    val formData: ExtensionForm = ExtensionForm.form.bindFromRequest.get

    val qry = if (formData.extension.isEmpty) {
      for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.id && fc.fName === cat.fName
      }) if catRow.isEmpty && fcRow.fSize > 0L
           } yield (fcRow.sha256, fcRow.fParent)
    } else {
      for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.id && fc.fName === cat.fName
      }) if catRow.isEmpty && fcRow.fSize > 0L && fcRow.fExtension.getOrElse("").toLowerCase === formData.extension.toLowerCase
           } yield (fcRow.sha256, fcRow.fParent)
    }
    database.runAsync(
      qry
        .groupBy(p => p._2)
        .map { case (fParent, cnt) => (fParent, cnt.map(_._2).length) }
        .sortBy(_._2.desc)
        .filterNot(_._1 endsWith "_files")
        .take(maxFilesTake)
        .result
    ).map { rowSeq =>
      Ok(views.html.smr_category_dir_by_ext(rowSeq))
    }
  }

}

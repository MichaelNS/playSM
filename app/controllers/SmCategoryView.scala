package controllers

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.DirWithoutCat
import models.db.Tables
import play.api.Logger
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

  val logger: Logger = play.api.Logger(getClass)

  /**
    * listCategoryTypeAndCnt
    *
    * @return [[views.html.category.smr_category]]
    */
  def listCategoryTypeAndCnt: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    debugParam
    database.runAsync(
      (for {
        ((fcRow, catRow), rulesRow) <- Tables.SmFileCard.joinLeft(Tables.SmCategoryFc).on((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
        })
          .joinLeft(Tables.SmCategoryRule).on(_._2.map(_.id) === _.id)

      } yield (fcRow.fSize, rulesRow.map(_.categoryType))
        )
        .filter(_._1 > 0L)
        .groupBy(p => p._2)
        .map { case (categoryType, cnt) => (categoryType, cnt.map(_._2).length) }
        .sortBy(_._2.desc)
        .result)
      .map { rowSeq =>
        Ok(views.html.category.smr_category_type(rowSeq, ExtensionForm.form))
      }
  }

  /**
    * listCategoryAndCnt
    *
    * @param categoryType categoryType
    * @return [[views.html.category.smr_category]]
    */
  def listCategoryAndCnt(categoryType: String): Action[AnyContent] = Action.async {
    database.runAsync(
      (for {
        ((fcRow, catRow), rulesRow) <- Tables.SmFileCard.join(Tables.SmCategoryFc).on((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
        })
          .joinLeft(Tables.SmCategoryRule).on(_._2.id === _.id)

      } yield (fcRow.fSize, rulesRow.map(_.categoryType), rulesRow.map(_.category))
        )
        .filter(_._2 === categoryType)
        .filter(_._1 > 0L)
        .groupBy(p => p._3)
        .map { case (description, cnt) => (description, cnt.map(_._3).length) }
        .sortBy(_._2.desc)
        .result)
      .map { rowSeq =>
        Ok(views.html.category.smr_category(categoryType, rowSeq)())
      }
  }

  /**
    * listSubCategoryAndCnt
    *
    * @param categoryType categoryType
    * @return [[views.html.category.smr_sub_category]]
    */
  def listSubCategoryAndCnt(categoryType: String, category: String): Action[AnyContent] = Action.async {
    database.runAsync(
      (for {
        ((fcRow, catRow), rulesRow)  <- Tables.SmFileCard.join (Tables.SmCategoryFc). on ((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
        })
          .joinLeft(Tables.SmCategoryRule).on(_._2.id === _.id)

      } yield (fcRow.fSize, rulesRow.map(_.categoryType), rulesRow.map(_.category), rulesRow.map(_.subCategory))
        )
        .filter(_._2 === categoryType)
        .filter(_._3 === category)
        .filter(_._1 > 0L)
        .groupBy(p => p._4)
        .map { case (subcategory, cnt) => (subcategory, cnt.map(_._4).length) }
        .sortBy(_._2.desc)
        .result)
      .map { rowSeq =>
        Ok(views.html.category.smr_sub_category(categoryType, category, rowSeq)())
      }
  }

  /**
    * get Dirs without category, order by lastdate
    *
    * @return [[views.html.category.cat_list_fc]]
    */
  def listDirWithoutCatByLastDate: Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxFilesTake: Long = config.getBytes("Category.maxFilesTake")
    val qry = (for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
      fc.sha256 === cat.sha256 && fc.fName === cat.fName
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

      Ok(views.html.category.cat_list_path(vView)())
    }
  }

  /**
    * get SmFileCard without category, order by lastdate
    *
    * @return [[views.html.category.cat_list_fc]]
    */
  def listFcWithoutCatByLastDate: Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxFilesTake: Long = config.getBytes("Category.maxFilesTake")

    val qry = (
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
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
      Ok(views.html.category.cat_list_fc(rowSeq)())
    }
  }


  /**
    * get Dirs without category by extension, order count files
    *
    * @return [[views.html.category.smr_category_dir_by_ext]]
    */
  def listDirWithoutCategoryByExtension(): Action[AnyContent] = Action.async { implicit request =>
    val config = ConfigFactory.load("scanImport.conf")
    val maxFilesTake: Long = config.getBytes("Category.maxFilesTake")

    val formData: ExtensionForm = ExtensionForm.form.bindFromRequest().get

    val qry = if (formData.extension.isEmpty) {
      for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.sha256 && fc.fName === cat.fName
      }) if catRow.isEmpty && fcRow.fSize > 0L
           } yield (fcRow.sha256, fcRow.fParent)
    } else {
      for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.sha256 && fc.fName === cat.fName
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
      Ok(views.html.category.smr_category_dir_by_ext(rowSeq)())
    }
  }

}

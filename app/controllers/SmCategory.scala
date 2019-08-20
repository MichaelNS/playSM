package controllers

import java.io.{IOException, InputStream}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.{Done, NotUsed}
import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import models.DirWithoutCat
import models.db.Tables
import org.camunda.bpm.dmn.engine.{DmnDecision, DmnDecisionTableResult, DmnEngineConfiguration}
import org.camunda.bpm.engine.variable.{VariableMap, Variables}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Constraints
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.db.DBService
import slick.basic.DatabasePublisher
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by ns on 13.03.2017.
  */
case class FormCategoryUpdate(categoryType: String, subCategoryType: String, description: String)

object FormCategoryUpdate {
  val form = Form(mapping(
    "category" -> text.verifying(Constraints.nonEmpty),
    "subcategory" -> text.verifying(Constraints.nonEmpty),
    "description" -> text.verifying(Constraints.nonEmpty)
  )(FormCategoryUpdate.apply)(FormCategoryUpdate.unapply))
}

@Singleton
class SmCategory @Inject()(val database: DBService)
  extends InjectedController {

  private val logger = Logger(classOf[SmCategory])

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  /**
    * listCategoryAndCnt
    *
    * @return [[views.html.smr_category]]
    */
  def listCategoryAndCnt: Action[AnyContent] = Action.async {
    database.runAsync(
      (for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        })} yield (fcRow, catRow.map(_.categoryType))
        )
        .groupBy(p => p._2)
        .map { case (categoryType, cnt) => (categoryType, cnt.map(_._2).length) }
        .sortBy(_._2.desc)
        .to[List].result)
      .map { rowSeq =>
        Ok(views.html.smr_category(rowSeq))
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
        .to[List].result)
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
        .to[List].result)
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
    * Call from [[SmReport.lstDirByDevice]] -> [[views.html.dirs_fc]]
    *
    * @param fParent  name dir for get SmFileCard
    * @param isBegins if true = get by startsWith, else by Equals
    * @return [[views.html.cat_fc_path]]
    */
  def listDirWithoutCatByParent(fParent: String,
                                isBegins: Boolean = false
                               ): Action[AnyContent] = Action.async {
    debugParam

    // query by startsWith
    val qry = if (isBegins) {
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        }) if fcRow.fParent.startsWith(fParent)
      } yield (fcRow.storeName, fcRow.fParent, fcRow.fName, fcRow.fLastModifiedDate, fcRow.sha256, catRow.map(_.categoryType), catRow.map(_.subCategoryType), catRow.map(_.description))

    }
    else {
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        }) if fcRow.fParent === fParent
      } yield (fcRow.storeName, fcRow.fParent, fcRow.fName, fcRow.fLastModifiedDate, fcRow.sha256, catRow.map(_.categoryType), catRow.map(_.subCategoryType), catRow.map(_.description))
    }

    database.runAsync(
      qry
        .sortBy(_._3)
        .sortBy(_._2)
        .sortBy(_._1)
        .result
    ).map { rowSeq =>
      val catForm: Form[FormCategoryUpdate] = Form(
        mapping(
          "categoryType" -> nonEmptyText,
          "subCategoryType" -> nonEmptyText,
          "description" -> nonEmptyText
        )(FormCategoryUpdate.apply)(FormCategoryUpdate.unapply)
      )

      Ok(views.html.cat_fc_path("path", fParent, rowSeq, catForm, isBegins))
    }
  }


  /**
    * get Dirs without category by extension, order count files
    *
    * @return [[views.html.smr_category_dir_by_ext]]
    */
  def listDirWithoutCategoryByExtension(extension: String): Action[AnyContent] = Action.async {
    val config = ConfigFactory.load("scanImport.conf")
    val maxFilesTake: Long = config.getBytes("Category.maxFilesTake")

    val qry = if (extension == "") {
      for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.id && fc.fName === cat.fName
      }) if catRow.isEmpty && fcRow.fSize > 0L
           } yield (fcRow.sha256, fcRow.fParent)
    } else {
      for {(fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
        fc.sha256 === cat.id && fc.fName === cat.fName
      }) if catRow.isEmpty && fcRow.fSize > 0L && fcRow.fExtension.getOrElse("").toLowerCase === extension
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

  /**
    * Form for assign Category And Description
    *
    * @param fParent  name dir for get SmFileCard
    * @param isBegins if true = get by startsWith, else by Equals
    * @return redirect [[SmCategory.listDirWithoutCatByParent]]
    */
  def assignCategoryAndDescription(fParent: String,
                                   isBegins: Boolean = false
                                  ): Action[AnyContent] = Action.async { implicit request =>
    FormCategoryUpdate.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.cat_form(formWithErrors, fParent, isBegins))),
      success = category => {
        if (category.categoryType.isEmpty || category.subCategoryType.isEmpty || category.description.isEmpty) {
          val form = FormCategoryUpdate.form.fill(category).withError("category", "categoryType isEmpty")
          Future.successful(BadRequest(views.html.cat_form(form, fParent, isBegins)))
        } else {
          batchAssignCategoryAndDescription(fParent, isBegins, category.categoryType, category.subCategoryType, category.description)

          Future.successful(Redirect(routes.SmCategory.listDirWithoutCatByParent(fParent, isBegins)))
        }
      }
    )
  }

  def batchAssignCategoryAndDescription(fParent: String,
                                        isBegins: Boolean = false,
                                        categoryType: String,
                                        subCategoryType: String,
                                        description: String
                                       ): Future[Done] = {
    debugParam

    val dbFcStream: Source[(Option[String], String), NotUsed] = getStreamFcByParent(fParent, isBegins)
    dbFcStream
      .throttle(elements = 50, 10.millisecond, maximumBurst = 1, ThrottleMode.shaping)
      .mapAsync(2)(writeToCategoryTbl(_, categoryType, subCategoryType, description))
      .runWith(Sink.ignore)
  }

  def applyRulesSetCategory: Action[AnyContent] = Action.async {
    val ruleFilePath = "/category.dmn"

    // create a new default DMN engine
    val dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration.buildEngine
    val inputStream: InputStream = getClass.getResourceAsStream(ruleFilePath)
    try {
      val decision: DmnDecision = dmnEngine.parseDecision("decision", inputStream)
      logger.info(s"decision.getKey = ${decision.getKey}   decision.getName = ${decision.getName}")

      val lstPath = getXmlRule(ruleFilePath)
      logger.info(s"lstPath = $lstPath")

      Source.fromIterator(() => lstPath.iterator)
        .throttle(elements = 1, 100.millisecond, maximumBurst = 2, ThrottleMode.shaping)
        .map { rulePath =>
          logger.info(s"rulePath = $rulePath")

          // prepare variables for decision evaluation
          val variables: VariableMap = Variables.putValue("path", rulePath)

          // evaluate decision
          val result: DmnDecisionTableResult = dmnEngine.evaluateDecisionTable(decision, variables)
          val outMap = result.getSingleResult.getEntryMap

          if (outMap.size() == 4) {
            batchAssignCategoryAndDescription(
              fParent = rulePath,
              isBegins = java.lang.Boolean.valueOf(outMap.get("isBegins").toString),
              categoryType = outMap.get("category").toString,
              subCategoryType = outMap.get("subcategory").toString,
              description = outMap.get("description").toString
            )
          } else {
            logger.warn(s"applyRules -> out DMN has < 4 values - $rulePath")
          }
        }
        .runWith(Sink.ignore)

      Future.successful(Ok("startRule"))
    } finally try
      inputStream.close()
    catch {
      case e: IOException => logger.error(s"Could not close stream: ${e.getMessage}")
    }
  }

  /**
    * read input param from DMN
    * replaceAll quotas need because NPE occurs when DMN exec - .getSingleResult.getSingleEntry.toString
    *
    * @param ruleFilePath List input DMN params
    * @return
    */
  def getXmlRule(ruleFilePath: String): Seq[String] = {
    val isDmn: InputStream = getClass.getResourceAsStream(ruleFilePath)

    try {
      val xml = scala.xml.XML.load(isDmn)
      (xml \\ "definitions" \\ "decision" \\ "decisionTable" \\ "rule" \\ "inputEntry" \\ "text").map(_.text.replaceAll("\"", ""))
    } finally try
      isDmn.close()
    catch {
      case e: IOException => logger.error(s"Could not close stream: ${e.getMessage}")
    }
  }

  // SmFileCard.sha256, SmFileCard.fName
  type DbRes = (Option[String], String)

  /**
    * Get stream SmFileCard from DB by dir name
    * https://stackoverflow.com/questions/44999614/stream-records-from-database-using-akka-stream
    *
    * @param fParent  name dir for get SmFileCard
    * @param isBegins if true = get by startsWith, else by Equals
    * @return Source[DbRes, NotUsed]
    */
  def getStreamFcByParent(fParent: String,
                          isBegins: Boolean = false
                         ): Source[DbRes, NotUsed] = {
    debugParam
    val preQuery = if (isBegins) {
      Tables.SmFileCard.filter(_.fParent startsWith fParent)
    } else {
      Tables.SmFileCard.filter(_.fParent === fParent)
    }
    val queryRes = preQuery.filter(_.sha256.nonEmpty).groupBy { p => (p.sha256, p.fName) }.map(fld => (fld._1._1, fld._1._2)).result
    val databasePublisher: DatabasePublisher[DbRes] = database runStream queryRes
    val akkaSourceFromSlick: Source[DbRes, NotUsed] = Source fromPublisher databasePublisher

    akkaSourceFromSlick
  }

  /**
    * upsert [[models.SmCategoryFc]]
    *
    * @param message         sha256 & fileName from SmFileCard
    * @param categoryType    categoryType
    * @param subCategoryType subCategoryType
    * @param description     description
    * @return count upsert records SmCategoryFc
    */
  def writeToCategoryTbl(message: (Option[String], String), categoryType: String, subCategoryType: String, description: String): Future[Int] = {
    val cRow = Tables.SmCategoryFcRow(message._1.get, message._2, Some(categoryType), Some(subCategoryType), Some(description))
    val insRes = database.runAsync(Tables.SmCategoryFc.insertOrUpdate(models.SmCategoryFc.apply(cRow).data.toRow))

    insRes
  }
}

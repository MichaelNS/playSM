package controllers

import java.io.{IOException, InputStream}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.{Done, NotUsed}
import javax.inject.{Inject, Singleton}
import models.db.Tables
import org.camunda.bpm.dmn.engine.{DmnDecision, DmnDecisionTableResult, DmnEngineConfiguration}
import org.camunda.bpm.engine.variable.{VariableMap, Variables}
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Constraints
import play.api.mvc._
import services.db.DBService
import slick.basic.DatabasePublisher
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by ns on 13.03.2017.
  */
case class FormCategoryUpdate(categoryType: String, category: String, subCategory: String, description: String)

object FormCategoryUpdate {
  val form = Form(mapping(
    "categoryType" -> text.verifying(Constraints.nonEmpty),
    "category" -> text.verifying(Constraints.nonEmpty),
    "subCategory" -> text.verifying(Constraints.nonEmpty),
    "description" -> text.verifying(Constraints.nonEmpty)
  )(FormCategoryUpdate.apply)(FormCategoryUpdate.unapply))
}

case class ExtensionForm(extension: String)

object ExtensionForm {
  val form: Form[ExtensionForm] = Form(
    mapping(
      "extension" -> text
    )(ExtensionForm.apply)(ExtensionForm.unapply)
  )
}


@Singleton
class SmCategory @Inject()(cc: MessagesControllerComponents, val database: DBService)
  extends MessagesAbstractController(cc)
    with play.api.i18n.I18nSupport {

  val logger = play.api.Logger(getClass)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()


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
      } yield (fcRow.deviceUid, fcRow.fParent, fcRow.fName, fcRow.fLastModifiedDate, fcRow.sha256, catRow.map(_.categoryType), catRow.map(_.category), catRow.map(_.subCategory), catRow.map(_.description))

    }
    else {
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        }) if fcRow.fParent === fParent
      } yield (fcRow.deviceUid, fcRow.fParent, fcRow.fName, fcRow.fLastModifiedDate, fcRow.sha256, catRow.map(_.categoryType), catRow.map(_.category), catRow.map(_.subCategory), catRow.map(_.description))
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
          "category" -> nonEmptyText,
          "subCategory" -> nonEmptyText,
          "description" -> nonEmptyText
        )(FormCategoryUpdate.apply)(FormCategoryUpdate.unapply)
      )

      Ok(views.html.cat_fc_path("path", fParent, rowSeq, catForm, isBegins))
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
        if (category.categoryType.isEmpty || category.category.isEmpty || category.subCategory.isEmpty) {
          val form = FormCategoryUpdate.form.fill(category).withError("category", "categoryType isEmpty")
          Future.successful(BadRequest(views.html.cat_form(form, fParent, isBegins)))
        } else {
          batchAssignCategoryAndDescription(fParent, isBegins, category.categoryType, category.category, category.subCategory, category.description)

          Future.successful(Redirect(routes.SmCategory.listDirWithoutCatByParent(fParent, isBegins)))
        }
      }
    )
  }

  def batchAssignCategoryAndDescription(fParent: String,
                                        isBegins: Boolean = false,
                                        categoryType: String,
                                        category: String,
                                        subCategory: String,
                                        description: String
                                       ): Future[Done] = {
    debugParam

    logger.info(s"start rulePath = $fParent")
    val start = System.currentTimeMillis

    val dbFcStream: Source[(Option[String], String), NotUsed] = getStreamFcByParent(fParent, isBegins)
    val applyRule = dbFcStream
      .throttle(elements = 500, 10.millisecond, maximumBurst = 1, mode = ThrottleMode.Shaping)
      .mapAsync(1)(writeToCategoryTbl(_, categoryType, category, subCategory, description))
      .runWith(Sink.ignore)

    applyRule.map(ll => logger.info(s"end rulePath = $fParent   ${System.currentTimeMillis - start} ms  $ll"))
    applyRule
  }

  case class Rule(fParent: String,
                  isBegins: Boolean,
                  categoryType: String,
                  category: String,
                  subCategory: String,
                  description: String
                 )

  def getRules: ArrayBuffer[Rule] = {
    val ruleFilePath = "/category.dmn"
    val lstRules: ArrayBuffer[Rule] = ArrayBuffer[Rule]()

    // create a new default DMN engine
    val dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration.buildEngine
    val inputStream: InputStream = getClass.getResourceAsStream(ruleFilePath)
    try {
      val decision: DmnDecision = dmnEngine.parseDecision("decision", inputStream)
      logger.info(s"decision.getKey = ${decision.getKey}   decision.getName = ${decision.getName}")
      val lstPath = getXmlRule(ruleFilePath)
      logger.info(s"lstPath = $lstPath")
      lstPath.foreach { rulePath =>
        // prepare variables for decision evaluation
        val variables: VariableMap = Variables.putValue("path", rulePath)
        // evaluate decision
        val result: DmnDecisionTableResult = dmnEngine.evaluateDecisionTable(decision, variables)
        val outMap = result.getSingleResult.getEntryMap
        if (outMap.size() == 4) {
          lstRules += Rule(rulePath, java.lang.Boolean.valueOf(outMap.get("isBegins").toString), outMap.get("categoryType").toString, outMap.get("category").toString, outMap.get("subcategory").toString, outMap.get("description").toString)
        } else {
          logger.warn(s"applyRules -> out DMN has < 4 values - $rulePath")
        }
      }
      lstRules
    }
    finally try
      inputStream.close()
    catch {
      case e: IOException => logger.error(s"Could not close stream: ${e.getMessage}")
    }
  }

  def applyRulesSetCategory: Action[AnyContent] = Action.async {
    Source.fromIterator(() => getRules.iterator)
      .throttle(elements = 1, 100.millisecond, maximumBurst = 1, mode = ThrottleMode.Shaping)
      .mapAsync(1) { rulePath =>
        batchAssignCategoryAndDescription(fParent = rulePath.fParent,
          isBegins = rulePath.isBegins,
          categoryType = rulePath.categoryType,
          category = rulePath.category,
          subCategory = rulePath.subCategory,
          description = rulePath.description)
      }
      .recover { case t: Throwable =>
        logger.error("Error retrieving output from flowA. Resuming without them.", t)
        None
      }
      .runWith(Sink.ignore)
      .onComplete {
        case Success(_) => logger.info("applyRulesSetCategory done ")
        case Failure(ex) => logger.error(s"applyRulesSetCategory error : ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
      }
    Future.successful(Redirect(routes.SmCategoryView.listCategoryAndCnt()))
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

    // query by startsWith
    val preQuery = if (isBegins) {
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        }) if fcRow.fParent.startsWith(fParent) && fcRow.sha256.nonEmpty && catRow.isEmpty
      } yield (fcRow.sha256, fcRow.fName)
    }
    else {
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.id && fc.fName === cat.fName
        }) if fcRow.fParent.startsWith(fParent) && fcRow.sha256.nonEmpty && catRow.isEmpty
      } yield (fcRow.sha256, fcRow.fName)
    }

    val queryRes = preQuery.result
    val databasePublisher: DatabasePublisher[DbRes] = database runStream queryRes
    val akkaSourceFromSlick: Source[DbRes, NotUsed] = Source fromPublisher databasePublisher

    akkaSourceFromSlick
  }

  /**
    * upsert [[models.SmCategoryFc]]
    *
    * @param message      sha256 & fileName from SmFileCard
    * @param categoryType categoryType
    * @param subCategory  subCategory
    * @param description  description
    * @return count upsert records SmCategoryFc
    */
  def writeToCategoryTbl(message: (Option[String], String), categoryType: String, category: String, subCategory: String, description: String): Future[Int] = {
    val cRow = Tables.SmCategoryFcRow(message._1.get, message._2, Some(categoryType), Some(category), Some(subCategory), Some(description))
    val insRes = database.runAsync(Tables.SmCategoryFc.insertOrUpdate(models.SmCategoryFc.apply(cRow).data.toRow))

    insRes
  }
}

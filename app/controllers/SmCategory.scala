package controllers

import java.io.{IOException, InputStream}

import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import javax.inject.{Inject, Singleton}
import models.SmCategoryRule
import models.db.Tables
import org.camunda.bpm.dmn.engine.{DmnDecision, DmnDecisionTableResult, DmnEngineConfiguration}
import org.camunda.bpm.engine.variable.{VariableMap, Variables}
import play.api.Logger
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
  val form: Form[FormCategoryUpdate] = Form(mapping(
    "categoryType" -> text.verifying(Constraints.nonEmpty),
    "category" -> text.verifying(Constraints.nonEmpty),
    "subCategory" -> text.verifying(Constraints.nonEmpty),
    "description" -> text
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

  val logger: Logger = play.api.Logger(getClass)

  implicit val system: ActorSystem = ActorSystem()


  /**
    * Call from [[SmReport.lstDirByDevice]] -> [[views.html.dirs_fc]]
    *
    * @param fParent  name dir for get SmFileCard
    * @param isBegins if true = get by startsWith, else by Equals
    * @return [[views.html.category.cat_fc_path]]
    */
  def listDirWithoutCatByParent(fParent: String,
                                isBegins: Boolean = false
                               ): Action[AnyContent] = Action.async {
    debugParam

    // query by startsWith
    val qry = if (isBegins) {
      for {
        ((fcRow, catRow), rulesRow) <- Tables.SmFileCard.joinLeft(Tables.SmCategoryFc).on((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
        }).joinLeft(Tables.SmCategoryRule).on(_._2.map(_.id) === _.id)

        if fcRow.fParent.startsWith(fParent)
      } yield (fcRow.deviceUid, fcRow.fParent, fcRow.fName, fcRow.fLastModifiedDate, fcRow.sha256, rulesRow.map(_.categoryType), rulesRow.map(_.category), rulesRow.map(_.subCategory), rulesRow.map(_.description))

    }
    else {
      for {
        ((fcRow, catRow), rulesRow) <- Tables.SmFileCard.joinLeft(Tables.SmCategoryFc).on((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
        }).joinLeft(Tables.SmCategoryRule).on(_._2.map(_.id) === _.id)
        if fcRow.fParent === fParent
      } yield (fcRow.deviceUid, fcRow.fParent, fcRow.fName, fcRow.fLastModifiedDate, fcRow.sha256, rulesRow.map(_.categoryType), rulesRow.map(_.category), rulesRow.map(_.subCategory), rulesRow.map(_.description))
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
          "description" -> text
        )(FormCategoryUpdate.apply)(FormCategoryUpdate.unapply)
      )

      Ok(views.html.category.cat_fc_path("path", fParent, rowSeq, catForm, isBegins))
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
      formWithErrors => Future.successful(BadRequest(views.html.category.cat_form(formWithErrors, fParent, isBegins))),
      success = category => {
        if (category.categoryType.isEmpty || category.category.isEmpty || category.subCategory.isEmpty) {
          val form = FormCategoryUpdate.form.fill(category).withError("category", "categoryType isEmpty")
          Future.successful(BadRequest(views.html.category.cat_form(form, fParent, isBegins)))
        } else {
          debug(category)
          writeCategoryToDb(fParent, isBegins, category)

          Future.successful(Redirect(routes.SmCategory.listDirWithoutCatByParent(fParent, isBegins)))
        }
      }
    )
  }

  def writeCategoryToDb(fParent: String, isBegins: Boolean = false, catForm: FormCategoryUpdate): Future[Any] = {
    val dbRes = database.runAsync(Tables.SmCategoryRule.filter(q => q.categoryType === catForm.categoryType && q.category === catForm.category && q.subCategory === catForm.subCategory).result)
      .map { rowSeq =>
        debug(rowSeq)
        if (rowSeq.isEmpty) {
          addCategoryRuleToDb(fParent, isBegins, catForm)
        } else {
          if (!rowSeq.head.fPath.toSet.contains(fParent)) {
            updateCategoryRuleInDb(fParent, isBegins, catForm, rowSeq.head.fPath.toSet + fParent)
          }
        }
      }
    dbRes.onComplete {
      case Success(_) => logger.info("writeCategoryToDb done ")
      case Failure(ex) => logger.error(s"writeCategoryToDb error : ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
    }

    dbRes
  }

  def addCategoryRuleToDb(fParent: String, isBegins: Boolean = false, catForm: FormCategoryUpdate): Future[Int] = {
    val cRow = Tables.SmCategoryRuleRow(-1, catForm.categoryType, catForm.category, catForm.subCategory, List(fParent), isBegins, if (catForm.description.nonEmpty) Some(catForm.description) else None)
    database.runAsync((Tables.SmCategoryRule returning Tables.SmCategoryRule.map(_.id)) += SmCategoryRule.apply(cRow).data.toRow)
  }

  def updateCategoryRuleInDb(fParent: String, isBegins: Boolean = false, catForm: FormCategoryUpdate, pathes: Set[String]): Future[Int] = {
    debug((pathes, fParent))

    val dbRes = database.runAsync((for {uRow <- Tables.SmCategoryRule if uRow.categoryType === catForm.categoryType && uRow.category === catForm.category && uRow.subCategory === catForm.subCategory} yield uRow.fPath)
      .update(pathes.toList))

    dbRes.map(_ => logger.info(s"update SmCategoryRule"))

    dbRes
  }

  def batchAssignCategoryAndDescription(fParent: String,
                                        isBegins: Boolean = false,
                                        id: Int
                                       ): Future[Done] = {
    debugParam

    logger.info(s"start rulePath = $fParent")
    val start = System.currentTimeMillis

    val dbFcStream: Source[(Option[String], String), NotUsed] = getStreamFcByParent(fParent, isBegins)
    val applyRule = dbFcStream
      .throttle(elements = 500, 10.millisecond, maximumBurst = 1, mode = ThrottleMode.Shaping)
      .mapAsync(1)(writeToCategoryTbl(_, id))
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

  @deprecated
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
          lstRules += Rule(rulePath, java.lang.Boolean.valueOf(outMap.get("isBegins").toString), outMap.get("category").toString, outMap.get("subcategory").toString, outMap.get("description").toString, "")
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

  def getStreamRulesFromDb: Source[Tables.SmCategoryRule#TableElementType, NotUsed] = {

    val queryRes = Tables.SmCategoryRule.result
    val databasePublisher: DatabasePublisher[Tables.SmCategoryRule#TableElementType] = database runStream queryRes
    val akkaSourceFromSlick: Source[Tables.SmCategoryRule#TableElementType, NotUsed] = Source fromPublisher databasePublisher

    akkaSourceFromSlick
  }

  def applyRulesSetCategory: Action[AnyContent] = Action.async {
    val dbFcStream: Source[Tables.SmCategoryRule#TableElementType, NotUsed] = getStreamRulesFromDb
    dbFcStream.throttle(elements = 1, 100.millisecond, maximumBurst = 1, mode = ThrottleMode.Shaping)
      .mapAsync(1) { rule =>
        debug(rule)
        val asd = rule.fPath.map { path =>
          batchAssignCategoryAndDescription(fParent = path,
            isBegins = rule.isBegins,
            id = rule.id
          )
        }
        // TODO fix to wait all futures
        asd.last
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
    Future.successful(Redirect(routes.SmCategoryView.listCategoryTypeAndCnt()))
  }


  /**
    * read input param from DMN
    * replaceAll quotas need because NPE occurs when DMN exec - .getSingleResult.getSingleEntry.toString
    *
    * @param ruleFilePath List input DMN params
    * @return
    */
  @deprecated
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
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
        }) if fcRow.fParent.startsWith(fParent) && fcRow.sha256.nonEmpty && catRow.isEmpty
      } yield (fcRow.sha256, fcRow.fName)
    }
    else {
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
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
    * @param message sha256 & fileName from SmFileCard
    * @param id      ID [[models.SmCategoryFc.id]]
    * @return count upsert records SmCategoryFc
    */
  def writeToCategoryTbl(message: (Option[String], String), id: Int): Future[Int] = {
    val cRow = Tables.SmCategoryFcRow(id, message._1.get, message._2)
    val insRes = database.runAsync(Tables.SmCategoryFc.insertOrUpdate(models.SmCategoryFc.apply(cRow).data.toRow))

    insRes
  }

  @deprecated
  def copyRulesToDb: Action[AnyContent] = Action.async {
    Source.fromIterator(() => getRules.iterator)
      .throttle(elements = 1, 100.millisecond, maximumBurst = 1, mode = ThrottleMode.Shaping)
      .mapAsync(1) { rulePath =>
        debug("-----------------------------------")
        debug("-----------------------------------")
        debug(rulePath)
        //        val dbRes =
        database.runAsync(Tables.SmCategoryRule.filter(q => q.categoryType === rulePath.categoryType && q.category === rulePath.category && q.subCategory === rulePath.subCategory).result)
          .map { rowSeq =>
            debug(rowSeq)
            if (rowSeq.isEmpty) {
              //              debug(rulePath.category, rulePath.subCategory, rulePath.description)
              val cRow = Tables.SmCategoryRuleRow(-1, rulePath.categoryType, rulePath.category, rulePath.subCategory, List(rulePath.fParent), rulePath.isBegins, None)
              //              debug(cRow)
              database.runAsync((Tables.SmCategoryRule returning Tables.SmCategoryRule.map(_.id)) += SmCategoryRule.apply(cRow).data.toRow)
            } else {
              if (!rowSeq.head.fPath.toSet.contains(rulePath.fParent)) {
                val pathes = rowSeq.head.fPath.toSet + rulePath.fParent
                //                pathes.+(rulePath.fParent)
                debug((pathes, rulePath.fParent))

                database.runAsync((for {uRow <- Tables.SmCategoryRule if uRow.categoryType === rulePath.categoryType && uRow.category === rulePath.category && uRow.subCategory === rulePath.subCategory} yield uRow.fPath)
                  .update(pathes.toList))
                  .map(_ => logger.info(s"update SmCategoryRule"))
              }
            }
          }
      }
      .recover { case t: Throwable =>
        logger.error("copyRulesToDb. Error retrieving output from file", t)
        None
      }
      .runWith(Sink.ignore)
      .onComplete {
        case Success(_) => logger.info("copyRulesToDb done ")
        case Failure(ex) => logger.error(s"copyRulesToDb error : ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
      }
    Future.successful(Ok("Done"))
  }

}

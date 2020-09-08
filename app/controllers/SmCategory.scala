package controllers

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import javax.inject.{Inject, Singleton}
import models.SmCategoryRule
import models.db.Tables
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Constraints
import play.api.mvc._
import services.db.DBService
import slick.basic.DatabasePublisher
import utils.db.SmPostgresDriver.api._

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

    val qry = getDirWithoutCatByParent(fParent, isBegins)
    database.runAsync(qry.result).map { rowSeq =>
      val catForm: Form[FormCategoryUpdate] = Form(
        mapping(
          "categoryType" -> nonEmptyText,
          "category" -> nonEmptyText,
          "subCategory" -> nonEmptyText,
          "description" -> text
        )(FormCategoryUpdate.apply)(FormCategoryUpdate.unapply)
      )

      val lstDevice = rowSeq.map(r => r._1).distinct
      val lstCat = rowSeq.map(r => FormCategoryUpdate(r._6.getOrElse(""), r._7.getOrElse(""), r._8.getOrElse(""), r._9.getOrElse("").toString)).distinct

      Ok(views.html.category.cat_fc_path("path", fParent, rowSeq, catForm, isBegins, lstDevice, lstCat)())
    }
  }

  def getDirWithoutCatByParent(fParent: String, isBegins: Boolean = false): Query[(
    Rep[String], Rep[String], Rep[String], Rep[LocalDateTime], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]],
      Rep[Option[Option[String]]]), (String, String, String, LocalDateTime, Option[String], Option[String], Option[String], Option[String], Option[Option[String]]), Seq] = {

    (Tables.SmDevice
      .join(Tables.SmFileCard) on (_.uid === _.deviceUid))
      //      .filter { case (_, fc) => (fc.sha256 =!= "" && (if (isBegins) fc.fParent.startsWith(fParent) else fc.fParent === fParent)) }
      //      .sortBy { case (device, fc) => (device.labelV, fc.fParent, fc.fName) }
      .joinLeft(Tables.SmCategoryFc).on { case ((_, fc), catFc) => fc.sha256 === catFc.sha256 && fc.fName === catFc.fName }
      .joinLeft(Tables.SmCategoryRule).on { case (((_, _), catFc), rule) => catFc.map(_.id) === rule.id }
      .map { case (((device, fc), _), rule) => (
        device.labelV,
        fc.fParent,
        fc.fName,
        fc.fLastModifiedDate,
        fc.sha256,
        rule.map(_.categoryType),
        rule.map(_.category),
        rule.map(_.subCategory),
        rule.map(_.description)
      )
      }
      .filter(if (isBegins) _._2.startsWith(fParent) else _._2 === fParent)
      .filter(_._5 =!= "")
      .sortBy(_._3)
      .sortBy(_._2)
      .sortBy(_._1)
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
    FormCategoryUpdate.form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.html.category.cat_form(formWithErrors, fParent, isBegins)())),
      success = category => {
        if (category.categoryType.isEmpty || category.category.isEmpty || category.subCategory.isEmpty) {
          val form = FormCategoryUpdate.form.fill(category).withError("category", "categoryType isEmpty")
          Future.successful(BadRequest(views.html.category.cat_form(form, fParent, isBegins)()))
        } else {
          debug(category)
          writeCategoryToDb(fParent, isBegins, category)

          Future.successful(Redirect(routes.SmCategory.listDirWithoutCatByParent(fParent, isBegins)))
        }
      }
    )
  }

  def writeCategoryToDb(fParent: String, isBegins: Boolean = false, catForm: FormCategoryUpdate): Future[Any] = {
    // TODO fix Future[Any] 2 Int
    val dbRes = database.runAsync(Tables.SmCategoryRule.filter(q => q.categoryType === catForm.categoryType && q.category === catForm.category && q.subCategory === catForm.subCategory).result)
      .map { rowSeq =>
        debug(rowSeq)
        if (rowSeq.isEmpty) {
          addCategoryRuleToDb(fParent, isBegins, catForm)
        } else {
          if (!rowSeq.head.fPath.toSet.contains(fParent)) {
            updateCategoryRuleInDb(fParent, catForm, rowSeq.head.fPath.toSet + fParent)
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
      .recover { case ex: Throwable =>
        logger.error(s"addCategoryRuleToDb error: ${ex.toString}\nStackTrace:\n ${ex.getStackTrace.mkString("\n")}")
        throw ex
      }
  }

  def updateCategoryRuleInDb(fParent: String, catForm: FormCategoryUpdate, pathes: Set[String]): Future[Int] = {
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

    val dbFcStream: Source[DbRes, NotUsed] = getStreamFcByParent(fParent, isBegins)
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
        // TODO check - fix to wait all futures
        Future.sequence(asd)
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

  // SmFileCard.sha256, SmFileCard.fName
  type DbRes = (Option[String], String, String)

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

    val preQuery = (
      for {
        (fcRow, catRow) <- Tables.SmFileCard joinLeft Tables.SmCategoryFc on ((fc, cat) => {
          fc.sha256 === cat.sha256 && fc.fName === cat.fName
        }) if fcRow.sha256.nonEmpty && catRow.isEmpty
      } yield (fcRow.sha256, fcRow.fName, fcRow.fParent)
      )
      .filterIf(isBegins)(_._3.startsWith(fParent))
      .filterIf(!isBegins)(_._3 === fParent)

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
  def writeToCategoryTbl(message: DbRes, id: Int): Future[Int] = {
    val cRow = Tables.SmCategoryFcRow(id, message._1.get, message._2)
    val insRes = database.runAsync(Tables.SmCategoryFc.insertOrUpdate(models.SmCategoryFc.apply(cRow).data.toRow))

    insRes
  }
}

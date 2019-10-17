package controllers

import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.google.common.hash.Hashing
import javax.inject.{Inject, Singleton}
import models.db.Tables
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.db.DBService
import slick.basic.DatabasePublisher
import utils.db.SmPostgresDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Migrate DB
  *
  * @param database database
  */
@Singleton
class SmMigrateDb @Inject()(val database: DBService)
  extends InjectedController {

  implicit val system: ActorSystem = ActorSystem()

  /**
    * Update ID in [[models.db.Tables.SmFileCard]]
    *
    * @param device device
    * @return
    */
  def convertDeviceIdBatch(device: String): Action[AnyContent] = Action.async {
    debug(device)

    val dbFcStream: Source[Tables.SmFileCard#TableElementType, NotUsed] = getStreamFcByStore(device)

    val dbRes = dbFcStream
      .grouped(1000)
      .mapAsync(1)(writeBatchToFcTbl)
      .runWith(Sink.ignore)

    dbRes.map(res => Ok("Job convert " + res))
  }

  def writeBatchToFcTbl(msg: Seq[Tables.SmFileCard#TableElementType]): Future[Int] = {
    debug(msg.size)

    val lstToIns = ArrayBuffer[Tables.SmFileCard#TableElementType]()
    val lstToDel = ArrayBuffer[String]()
    msg.foreach { p =>
      val newId = Hashing.sha256().hashString(p.deviceUid + p.fParent + p.fName, StandardCharsets.UTF_8).toString.toUpperCase

      if (p.id != newId) {
        lstToIns += Tables.SmFileCardRow(newId, p.deviceUid, p.fParent, p.fName, p.fExtension,
          p.fCreationDate, p.fLastModifiedDate, p.fSize, p.fMimeTypeJava, p.sha256, p.fNameLc
        )
        lstToDel += p.id
      }
    }
    debug(lstToIns.size)
    if (lstToIns.nonEmpty) {
      val insRes = database.runAsync((Tables.SmFileCard returning Tables.SmFileCard.map(_.id)) ++= lstToIns)

      // delete
      lstToDel.foreach { id =>
        database.runAsync(Tables.SmFileCard.filter(_.id === id).delete)
        //        val delRes = database.runAsync(Tables.SmFileCard.filter(_.id === id).delete)
        //        delRes map { resDel => }
      }
      insRes map { resIns =>
        resIns.size
      }
    } else {
      Future.successful(0)
    }
  }

  def getStreamFcByStore(device: String): Source[Tables.SmFileCard#TableElementType, NotUsed] = {

    val queryRes = Tables.SmFileCard.filter(_.deviceUid === device).result
    val databasePublisher: DatabasePublisher[Tables.SmFileCard#TableElementType] = database runStream queryRes
    val akkaSourceFromSlick: Source[Tables.SmFileCard#TableElementType, NotUsed] = Source fromPublisher databasePublisher

    akkaSourceFromSlick
  }
}

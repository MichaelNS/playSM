package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{JsPath, Json, Writes}
import play.api.mvc.{Action, _}
import play.api.{Configuration, Logger}
import ru.ns.model.OsConf
import ru.ns.tools.FileUtils
import services.db.DBService

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by ns on 23.01.2017.
  */
@Singleton
class SmSyncCmpDir @Inject()(implicit assetsFinder: AssetsFinder, config: Configuration, val database: DBService)
  extends InjectedController {

  val logger: Logger = play.api.Logger(getClass)

  case class State(opened: Boolean = false,
                   disabled: Boolean = false,
                   selected: Boolean = false
                  ) {
  }

  case class Root(id: String,
                  text: String,
                  icon: String,
                  children: Boolean,
                  state: State
                 ) {
  }

  implicit val stateWrites: Writes[State] = (
    (JsPath \ "opened").write[Boolean] and
      (JsPath \ "disabled").write[Boolean] and
      (JsPath \ "selected").write[Boolean]
    ) (unlift(State.unapply))

  implicit val filePathWrites: Writes[Root] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "text").write[String] and
      (JsPath \ "icon").write[String] and
      (JsPath \ "children").write[Boolean] and
      (JsPath \ "state").write[State]
    ) (unlift(Root.unapply))

  def getChildren(deviceUid: String, path: String = ""): Action[AnyContent] = Action.async {
    debug((deviceUid, path))

    val roots = ArrayBuffer[Root]()

    FileUtils.getDeviceInfo(deviceUid).map { device =>
      if (device.isDefined) {
        val impPaths: Seq[String] = config.get[Seq[String]]("paths2Scan.volumes." + deviceUid)
        debug(s"${impPaths.length} : $impPaths")
        logger.debug(pprint.apply(impPaths.map(c => device.get.mountpoint + OsConf.fsSeparator + c)).toString())

        val lstDirs = FileUtils.getPathChildren(if (path == "#") OsConf.fsSeparator else path, device.get.mountpoint, 2)
        debug(lstDirs._1)
        if (lstDirs._2.nonEmpty) logger.warn(s"AccessDeniedException ${pprint.apply(lstDirs._2.map(_.fParent).toString()).toString()}")

        // AccessDenied icon mark
        lstDirs._2.foreach(dir => roots += Root(dir.fParent, dir.fParent, assetsFinder.path("images/jstree/stop.png"), children = false, State()))

        lstDirs._1.foreach { dir =>
          val dirSplitedList: List[String] = dir.fParent.split("/").map(_.trim).toList
          roots += Root(dir.fParent, dirSplitedList.last, getIcon(dir.fParent, impPaths), children = true, State(opened = false, disabled = false, impPaths.contains(dir.fParent)))
        }

        Ok(Json.toJson(roots))
      } else {
        Ok("device mountPoint is empty")
      }
    }
  }

  def getIcon(path: String, impPaths: Seq[String]): String = {
    if (impPaths.exists(_.startsWith(s"$path/"))) assetsFinder.path("images/jstree/arrow_right.png") else ""
  }
}

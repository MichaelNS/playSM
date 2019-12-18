package tasks

import akka.actor.ActorSystem
import controllers.SmSyncDeviceStream
import javax.inject.Inject
import play.api.Configuration
import play.api.mvc.MessagesControllerComponents
import services.db.DBService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class CalcCrcTask @Inject()(actorSystem: ActorSystem, cc: MessagesControllerComponents, config: Configuration, val database: DBService)(implicit executionContext: ExecutionContext) {
  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 1.minute, interval = 10.minute) { () =>
    // the block of code that will be executed
    actorSystem.log.info("Executing something...")

    new SmSyncDeviceStream(cc, config, database).calcAllCRCActor()
  }
}
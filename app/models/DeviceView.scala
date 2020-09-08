package models

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}

case class DeviceView(name: String,
                      label: String,
                      uid: String,
                      description: String,
                      syncDate: LocalDateTime,
                      visible: Boolean,
                      reliable: Boolean,
                      withOutCrc: Int,
                      syncDateDiff: String,
                      isSynced: Boolean
                     ) {

}

object DeviceView {
  def apply(name: String, label: String, uid: String, description: Option[String], syncDate: Option[LocalDateTime], visible: Boolean, reliable: Boolean, withOutCrc: Int): DeviceView = {
    val syncDateDiff: String = if (syncDate.isDefined) {
      syncDate.get.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " (" +
        ChronoUnit.DAYS.between(syncDate.get.toLocalDate, LocalDate.now()).toString + " days ago)"
    }
    else {
      "None"
    }

    new DeviceView(name,
      label,
      uid,
      description.getOrElse(""),
      syncDate.getOrElse(LocalDateTime.MIN),
      visible,
      reliable,
      withOutCrc,
      syncDateDiff,
      syncDate.isDefined)
  }
}

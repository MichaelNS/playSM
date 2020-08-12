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
  def isDeviceSynced(syncDate: LocalDateTime): Boolean = {
    syncDate.getYear > -1
  }

  def apply(name: String, label: String, uid: String, description: String, syncDate: LocalDateTime, visible: Boolean, reliable: Boolean, withOutCrc: Int): DeviceView = {
    val syncDateDiff: String = if (isDeviceSynced(syncDate)) {
      syncDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " (" +
        ChronoUnit.DAYS.between(syncDate.toLocalDate, LocalDate.now()).toString + " days ago)"
    }
    else {
      "None"
    }
    val isSynced: Boolean = if (isDeviceSynced(syncDate)) true else false

    new DeviceView(name, label, uid, description, syncDate, visible, reliable, withOutCrc, syncDateDiff, isSynced)
  }
}

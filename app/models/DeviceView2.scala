package models

import java.time.LocalDateTime

case class DeviceView2(name: String,
                      label: String,
                      uid: String,
                      description: String,
                      syncDate: LocalDateTime,
                      visible: Boolean,
                      reliable: Boolean,
                      withOutCrc: Int
                     ) {

}

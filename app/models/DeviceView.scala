package models

import org.joda.time.DateTime

case class DeviceView(name: String,
                      label: String,
                      uid: String,
                      describe: String,
                      syncDate: DateTime,
                      visible: Boolean,
                      withOutCrc: Int
                     ) {

}

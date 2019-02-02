package ru.ns.model

/**
  * Created by ns on 22.08.2019.
  */
case class SmExif(dateTime: Option[java.time.LocalDateTime] = None,
                  dateTimeOriginal: Option[java.time.LocalDateTime] = None,
                  dateTimeDigitized: Option[java.time.LocalDateTime] = None,
                  make: Option[String] = None,
                  model: Option[String] = None,
                  software: Option[String] = None,
                  exifImageWidth: Option[String] = None,
                  exifImageHeight: Option[String] = None
                 )


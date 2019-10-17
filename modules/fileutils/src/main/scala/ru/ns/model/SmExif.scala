package ru.ns.model

import com.drew.lang.GeoLocation

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
                  exifImageHeight: Option[String] = None,
                  gpsVersionID: Option[String] = None,
                  gpsLatitudeRef: Option[String] = None,
                  gpsLatitude: Option[String] = None,
                  gpsLongitudeRef: Option[String] = None,
                  gpsLongitude: Option[String] = None,
                  gpsAltitudeRef: Option[String] = None,
                  gpsAltitude: Option[String] = None,
                  gpsTimeStamp: Option[String] = None,
                  gpsProcessingMethod: Option[String] = None,
                  gpsDateStamp: Option[String] = None,
                  gpsLatitudeD: Option[BigDecimal] = None,
                  gpsLongitudeD: Option[BigDecimal] = None
                 )

case class SmExifGoo(fullPath: String,
                     geoLocation: GeoLocation
                    )

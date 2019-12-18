package models

import models.db.Tables

case class Entity[T](id: Int, data: T)

case class EntitySmFc[T](id: String, data: T)

case class SmDevice(name: String, label: String, uid: String, syncDate: java.time.LocalDateTime) {
  def toRow: Tables.SmDeviceRow = {
    Tables.SmDeviceRow(
      id = -1,
      name = name,
      label = label,
      uid = uid,
      syncDate = syncDate
    )
  }
}

object SmDevice {
  def apply(row: Tables.SmDeviceRow): Entity[SmDevice] = {
    Entity(
      id = row.id,
      data = SmDevice(
        name = row.name,
        row.label,
        uid = row.uid,
        row.syncDate
      )
    )
  }

}

case class SmFileCard(
                       id: String,
                       deviceUid: String,
                       fParent: String,
                       fName: String,
                       fExtension: Option[String] = None,
                       fCreationDate: java.time.LocalDateTime,
                       fLastModifiedDate: java.time.LocalDateTime,
                       fSize: Option[Long] = None,
                       fMimeTypeJava: Option[String] = None,
                       sha256: Option[String] = None,
                       fNameLc: String
                     ) {
  def toRow: _root_.models.db.Tables.SmFileCardRow = {
    Tables.SmFileCardRow(
      id = id,
      deviceUid = deviceUid,
      fParent = fParent,
      fName = fName,
      fExtension = fExtension,
      fCreationDate = fCreationDate,
      fLastModifiedDate = fLastModifiedDate,
      fSize = fSize,
      fMimeTypeJava = fMimeTypeJava,
      sha256 = sha256,
      fNameLc = fNameLc,
    )
  }
}

object SmFileCard {
  def apply(row: Tables.SmFileCardRow): EntitySmFc[SmFileCard] = {
    EntitySmFc(
      id = row.id,
      data = SmFileCard(
        id = row.id,
        deviceUid = row.deviceUid,
        fParent = row.fParent,
        fName = row.fName,
        fExtension = row.fExtension,
        fCreationDate = row.fCreationDate,
        fLastModifiedDate = row.fLastModifiedDate,
        fSize = row.fSize,
        fMimeTypeJava = row.fMimeTypeJava,
        sha256 = row.sha256,
        fNameLc = row.fNameLc
      )
    )
  }
}

case class SmCategoryFc(
                         id: String,
                         fName: String,
                         categoryType: Option[String] = None,
                         subCategoryType: Option[String] = None,
                         description: Option[String] = None
                       ) {
  def toRow: Tables.SmCategoryFcRow = {
    Tables.SmCategoryFcRow(
      id = id,
      fName = fName,
      categoryType = categoryType,
      subCategoryType = subCategoryType,
      description = description
    )
  }
}

object SmCategoryFc {
  def apply(row: Tables.SmCategoryFcRow): EntitySmFc[SmCategoryFc] = {
    EntitySmFc(
      id = row.id,
      data = SmCategoryFc(
        id = row.id,
        fName = row.fName,
        categoryType = row.categoryType,
        subCategoryType = row.subCategoryType,
        description = row.description
      )
    )
  }
}

case class SmExif(id: String,
                  dateTime: Option[java.time.LocalDateTime] = None,
                  dateTimeOriginal: Option[java.time.LocalDateTime] = None,
                  dateTimeDigitized: Option[java.time.LocalDateTime] = None,
                  make: Option[String] = None,
                  model: Option[String] = None,
                  software: Option[String] = None,
                  exifImageWidth: Option[String] = None,
                  exifImageHeight: Option[String] = None,
                  gpsVersionId: Option[String] = None,
                  gpsLatitudeRef: Option[String] = None,
                  gpsLatitude: Option[String] = None,
                  gpsLongitudeRef: Option[String] = None,
                  gpsLongitude: Option[String] = None,
                  gpsAltitudeRef: Option[String] = None,
                  gpsAltitude: Option[String] = None,
                  gpsTimeStamp: Option[String] = None,
                  gpsProcessingMethod: Option[String] = None,
                  gpsDateStamp: Option[String] = None,
                  gpsLatitudeDec: Option[scala.math.BigDecimal],
                  gpsLongitudeDec: Option[scala.math.BigDecimal]
                 ) {
  def toRow: Tables.SmExifRow = {
    Tables.SmExifRow(
      id = id,
      dateTime = dateTime,
      dateTimeOriginal = dateTimeOriginal,
      dateTimeDigitized = dateTimeDigitized,
      make = make,
      model = model,
      software = software,
      exifImageWidth = exifImageWidth,
      exifImageHeight = exifImageHeight,
      gpsVersionId = gpsVersionId,
      gpsLatitudeRef = gpsLatitudeRef,
      gpsLatitude = gpsLatitude,
      gpsLongitudeRef = gpsLongitudeRef,
      gpsLongitude = gpsLongitude,
      gpsAltitudeRef = gpsAltitudeRef,
      gpsAltitude = gpsAltitude,
      gpsTimeStamp = gpsTimeStamp,
      gpsProcessingMethod = gpsProcessingMethod,
      gpsDateStamp = gpsDateStamp,
      gpsLatitudeDec = gpsLatitudeDec,
      gpsLongitudeDec = gpsLongitudeDec
    )
  }
}

object SmExif {
  def apply(row: Tables.SmExifRow): EntitySmFc[SmExif] = {
    EntitySmFc(
      id = row.id,
      data = SmExif(
        id = row.id,
        dateTime = row.dateTime,
        dateTimeOriginal = row.dateTimeOriginal,
        dateTimeDigitized = row.dateTimeDigitized,
        make = row.make,
        model = row.model,
        software = row.software,
        exifImageWidth = row.exifImageWidth,
        exifImageHeight = row.exifImageHeight,
        gpsVersionId = row.gpsVersionId,
        gpsLatitudeRef = row.gpsLatitudeRef,
        gpsLatitude = row.gpsLatitude,
        gpsLongitudeRef = row.gpsLongitudeRef,
        gpsLongitude = row.gpsLongitude,
        gpsAltitudeRef = row.gpsAltitudeRef,
        gpsAltitude = row.gpsAltitude,
        gpsTimeStamp = row.gpsTimeStamp,
        gpsProcessingMethod = row.gpsProcessingMethod,
        gpsDateStamp = row.gpsDateStamp,
        gpsLatitudeDec = row.gpsLatitudeDec,
        gpsLongitudeDec = row.gpsLongitudeDec
      )
    )
  }
}



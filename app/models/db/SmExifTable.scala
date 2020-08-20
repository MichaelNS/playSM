package models.db

// AUTO-GENERATED Slick data model for table SmExif
trait SmExifTable {

  self: Tables =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmExif
    *
    * @param id                  Database column id SqlType(varchar), PrimaryKey
    * @param dateTime            Database column date_time SqlType(timestamp), Default(None)
    * @param dateTimeOriginal    Database column date_time_original SqlType(timestamp), Default(None)
    * @param dateTimeDigitized   Database column date_time_digitized SqlType(timestamp), Default(None)
    * @param make                Database column make SqlType(varchar), Default(None)
    * @param model               Database column model SqlType(varchar), Default(None)
    * @param software            Database column software SqlType(varchar), Default(None)
    * @param exifImageWidth      Database column exif_image_width SqlType(varchar), Default(None)
    * @param exifImageHeight     Database column exif_image_height SqlType(varchar), Default(None)
    * @param gpsVersionId        Database column gps_version_id SqlType(varchar), Default(None)
    * @param gpsLatitudeRef      Database column gps_latitude_ref SqlType(varchar), Default(None)
    * @param gpsLatitude         Database column gps_latitude SqlType(varchar), Default(None)
    * @param gpsLongitudeRef     Database column gps_longitude_ref SqlType(varchar), Default(None)
    * @param gpsLongitude        Database column gps_longitude SqlType(varchar), Default(None)
    * @param gpsAltitudeRef      Database column gps_altitude_ref SqlType(varchar), Default(None)
    * @param gpsAltitude         Database column gps_altitude SqlType(varchar), Default(None)
    * @param gpsTimeStamp        Database column gps_time_stamp SqlType(varchar), Default(None)
    * @param gpsProcessingMethod Database column gps_processing_method SqlType(varchar), Default(None)
    * @param gpsDateStamp        Database column gps_date_stamp SqlType(varchar), Default(None)
    * @param gpsLatitudeDec      Database column gps_latitude_dec SqlType(numeric), Default(None)
    * @param gpsLongitudeDec     Database column gps_longitude_dec SqlType(numeric), Default(None) */
  case class SmExifRow(id: String, dateTime: Option[java.time.LocalDateTime] = None, dateTimeOriginal: Option[java.time.LocalDateTime] = None, dateTimeDigitized: Option[java.time.LocalDateTime] = None, make: Option[String] = None, model: Option[String] = None, software: Option[String] = None, exifImageWidth: Option[String] = None, exifImageHeight: Option[String] = None, gpsVersionId: Option[String] = None, gpsLatitudeRef: Option[String] = None, gpsLatitude: Option[String] = None, gpsLongitudeRef: Option[String] = None, gpsLongitude: Option[String] = None, gpsAltitudeRef: Option[String] = None, gpsAltitude: Option[String] = None, gpsTimeStamp: Option[String] = None, gpsProcessingMethod: Option[String] = None, gpsDateStamp: Option[String] = None, gpsLatitudeDec: Option[scala.math.BigDecimal] = None, gpsLongitudeDec: Option[scala.math.BigDecimal] = None)

  /** GetResult implicit for fetching SmExifRow objects using plain SQL queries */
  implicit def GetResultSmExifRow(implicit e0: GR[String], e1: GR[Option[java.time.LocalDateTime]], e2: GR[Option[String]], e3: GR[Option[scala.math.BigDecimal]]): GR[SmExifRow] = GR {
    prs =>
      import prs._
      SmExifRow.tupled((<<[String], <<?[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }

  /** Table description of table sm_exif. Objects of this class serve as prototypes for rows in queries. */
  class SmExif(_tableTag: Tag) extends profile.api.Table[SmExifRow](_tableTag, "sm_exif") {
    def * = (id, dateTime, dateTimeOriginal, dateTimeDigitized, make, model, software, exifImageWidth, exifImageHeight, gpsVersionId, gpsLatitudeRef, gpsLatitude, gpsLongitudeRef, gpsLongitude, gpsAltitudeRef, gpsAltitude, gpsTimeStamp, gpsProcessingMethod, gpsDateStamp, gpsLatitudeDec, gpsLongitudeDec) <> (SmExifRow.tupled, SmExifRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), dateTime, dateTimeOriginal, dateTimeDigitized, make, model, software, exifImageWidth, exifImageHeight, gpsVersionId, gpsLatitudeRef, gpsLatitude, gpsLongitudeRef, gpsLongitude, gpsAltitudeRef, gpsAltitude, gpsTimeStamp, gpsProcessingMethod, gpsDateStamp, gpsLatitudeDec, gpsLongitudeDec).shaped.<>({ r => import r._; _1.map(_ => SmExifRow.tupled((_1.get, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20, _21))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(varchar), PrimaryKey */
    val id: Rep[String] = column[String]("id", O.PrimaryKey)
    /** Database column date_time SqlType(timestamp), Default(None) */
    val dateTime: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("date_time", O.Default(None))
    /** Database column date_time_original SqlType(timestamp), Default(None) */
    val dateTimeOriginal: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("date_time_original", O.Default(None))
    /** Database column date_time_digitized SqlType(timestamp), Default(None) */
    val dateTimeDigitized: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("date_time_digitized", O.Default(None))
    /** Database column make SqlType(varchar), Default(None) */
    val make: Rep[Option[String]] = column[Option[String]]("make", O.Default(None))
    /** Database column model SqlType(varchar), Default(None) */
    val model: Rep[Option[String]] = column[Option[String]]("model", O.Default(None))
    /** Database column software SqlType(varchar), Default(None) */
    val software: Rep[Option[String]] = column[Option[String]]("software", O.Default(None))
    /** Database column exif_image_width SqlType(varchar), Default(None) */
    val exifImageWidth: Rep[Option[String]] = column[Option[String]]("exif_image_width", O.Default(None))
    /** Database column exif_image_height SqlType(varchar), Default(None) */
    val exifImageHeight: Rep[Option[String]] = column[Option[String]]("exif_image_height", O.Default(None))
    /** Database column gps_version_id SqlType(varchar), Default(None) */
    val gpsVersionId: Rep[Option[String]] = column[Option[String]]("gps_version_id", O.Default(None))
    /** Database column gps_latitude_ref SqlType(varchar), Default(None) */
    val gpsLatitudeRef: Rep[Option[String]] = column[Option[String]]("gps_latitude_ref", O.Default(None))
    /** Database column gps_latitude SqlType(varchar), Default(None) */
    val gpsLatitude: Rep[Option[String]] = column[Option[String]]("gps_latitude", O.Default(None))
    /** Database column gps_longitude_ref SqlType(varchar), Default(None) */
    val gpsLongitudeRef: Rep[Option[String]] = column[Option[String]]("gps_longitude_ref", O.Default(None))
    /** Database column gps_longitude SqlType(varchar), Default(None) */
    val gpsLongitude: Rep[Option[String]] = column[Option[String]]("gps_longitude", O.Default(None))
    /** Database column gps_altitude_ref SqlType(varchar), Default(None) */
    val gpsAltitudeRef: Rep[Option[String]] = column[Option[String]]("gps_altitude_ref", O.Default(None))
    /** Database column gps_altitude SqlType(varchar), Default(None) */
    val gpsAltitude: Rep[Option[String]] = column[Option[String]]("gps_altitude", O.Default(None))
    /** Database column gps_time_stamp SqlType(varchar), Default(None) */
    val gpsTimeStamp: Rep[Option[String]] = column[Option[String]]("gps_time_stamp", O.Default(None))
    /** Database column gps_processing_method SqlType(varchar), Default(None) */
    val gpsProcessingMethod: Rep[Option[String]] = column[Option[String]]("gps_processing_method", O.Default(None))
    /** Database column gps_date_stamp SqlType(varchar), Default(None) */
    val gpsDateStamp: Rep[Option[String]] = column[Option[String]]("gps_date_stamp", O.Default(None))
    /** Database column gps_latitude_dec SqlType(numeric), Default(None) */
    val gpsLatitudeDec: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("gps_latitude_dec", O.Default(None))
    /** Database column gps_longitude_dec SqlType(numeric), Default(None) */
    val gpsLongitudeDec: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("gps_longitude_dec", O.Default(None))

    /** Foreign key referencing SmFileCard (database name fk_sm_exif_sm_file_card) */
    lazy val smFileCardFk = foreignKey("fk_sm_exif_sm_file_card", id, SmFileCard)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table SmExif */
  lazy val SmExif = new TableQuery(tag => new SmExif(tag))
}

package models.db

// AUTO-GENERATED Slick data model [2019-11-28T10:18:43.200+03:00[Europe/Moscow]]

/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = utils.db.SmPostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: utils.db.SmPostgresDriver

  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = SmCategoryFc.schema ++ SmDevice.schema ++ SmExif.schema ++ SmFileCard.schema ++ SmPathMove.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table SmCategoryFc
    *
    * @param id              Database column id SqlType(varchar)
    * @param fName           Database column f_name SqlType(varchar)
    * @param categoryType    Database column category_type SqlType(varchar), Default(None)
    * @param subCategoryType Database column sub_category_type SqlType(varchar), Default(None)
    * @param description     Database column description SqlType(varchar), Default(None) */
  case class SmCategoryFcRow(id: String, fName: String, categoryType: Option[String] = None, subCategoryType: Option[String] = None, description: Option[String] = None)

  /** GetResult implicit for fetching SmCategoryFcRow objects using plain SQL queries */
  implicit def GetResultSmCategoryFcRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[SmCategoryFcRow] = GR {
    prs =>
      import prs._
      SmCategoryFcRow.tupled((<<[String], <<[String], <<?[String], <<?[String], <<?[String]))
  }

  /** Table description of table sm_category_fc. Objects of this class serve as prototypes for rows in queries. */
  class SmCategoryFc(_tableTag: Tag) extends profile.api.Table[SmCategoryFcRow](_tableTag, "sm_category_fc") {
    def * = (id, fName, categoryType, subCategoryType, description) <> (SmCategoryFcRow.tupled, SmCategoryFcRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(fName), categoryType, subCategoryType, description)).shaped.<>({ r => import r._; _1.map(_ => SmCategoryFcRow.tupled((_1.get, _2.get, _3, _4, _5))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(varchar) */
    val id: Rep[String] = column[String]("id")
    /** Database column f_name SqlType(varchar) */
    val fName: Rep[String] = column[String]("f_name")
    /** Database column category_type SqlType(varchar), Default(None) */
    val categoryType: Rep[Option[String]] = column[Option[String]]("category_type", O.Default(None))
    /** Database column sub_category_type SqlType(varchar), Default(None) */
    val subCategoryType: Rep[Option[String]] = column[Option[String]]("sub_category_type", O.Default(None))
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))

    /** Primary key of SmCategoryFc (database name sm_category_fc_pkey) */
    val pk = primaryKey("sm_category_fc_pkey", (id, fName))
  }

  /** Collection-like TableQuery object for table SmCategoryFc */
  lazy val SmCategoryFc = new TableQuery(tag => new SmCategoryFc(tag))

  /** Entity class storing rows of table SmDevice
    *
    * @param id       Database column id SqlType(serial), AutoInc, PrimaryKey
    * @param name     Database column name SqlType(varchar)
    * @param label    Database column label SqlType(varchar)
    * @param uid      Database column uid SqlType(varchar)
    * @param syncDate Database column sync_date SqlType(timestamp)
    * @param describe Database column describe SqlType(varchar), Default(None)
    * @param visible  Database column visible SqlType(bool), Default(true)
    * @param reliable Database column reliable SqlType(bool), Default(true) */
  case class SmDeviceRow(id: Int, name: String, label: String, uid: String, syncDate: java.time.LocalDateTime, describe: Option[String] = None, visible: Boolean = true, reliable: Boolean = true)

  /** GetResult implicit for fetching SmDeviceRow objects using plain SQL queries */
  implicit def GetResultSmDeviceRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.time.LocalDateTime], e3: GR[Option[String]], e4: GR[Boolean]): GR[SmDeviceRow] = GR {
    prs =>
      import prs._
      SmDeviceRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[java.time.LocalDateTime], <<?[String], <<[Boolean], <<[Boolean]))
  }

  /** Table description of table sm_device. Objects of this class serve as prototypes for rows in queries. */
  class SmDevice(_tableTag: Tag) extends profile.api.Table[SmDeviceRow](_tableTag, "sm_device") {
    def * = (id, name, label, uid, syncDate, describe, visible, reliable) <> (SmDeviceRow.tupled, SmDeviceRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(label), Rep.Some(uid), Rep.Some(syncDate), describe, Rep.Some(visible), Rep.Some(reliable))).shaped.<>({ r => import r._; _1.map(_ => SmDeviceRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7.get, _8.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column label SqlType(varchar) */
    val label: Rep[String] = column[String]("label")
    /** Database column uid SqlType(varchar) */
    val uid: Rep[String] = column[String]("uid")
    /** Database column sync_date SqlType(timestamp) */
    val syncDate: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("sync_date")
    /** Database column describe SqlType(varchar), Default(None) */
    val describe: Rep[Option[String]] = column[Option[String]]("describe", O.Default(None))
    /** Database column visible SqlType(bool), Default(true) */
    val visible: Rep[Boolean] = column[Boolean]("visible", O.Default(true))
    /** Database column reliable SqlType(bool), Default(true) */
    val reliable: Rep[Boolean] = column[Boolean]("reliable", O.Default(true))
  }

  /** Collection-like TableQuery object for table SmDevice */
  lazy val SmDevice = new TableQuery(tag => new SmDevice(tag))

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
    def ? = ((Rep.Some(id), dateTime, dateTimeOriginal, dateTimeDigitized, make, model, software, exifImageWidth, exifImageHeight, gpsVersionId, gpsLatitudeRef, gpsLatitude, gpsLongitudeRef, gpsLongitude, gpsAltitudeRef, gpsAltitude, gpsTimeStamp, gpsProcessingMethod, gpsDateStamp, gpsLatitudeDec, gpsLongitudeDec)).shaped.<>({ r => import r._; _1.map(_ => SmExifRow.tupled((_1.get, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20, _21))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

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
  }

  /** Collection-like TableQuery object for table SmExif */
  lazy val SmExif = new TableQuery(tag => new SmExif(tag))

  /** Entity class storing rows of table SmFileCard
    *
    * @param id                Database column id SqlType(varchar), PrimaryKey
    * @param storeName         Database column store_name SqlType(varchar)
    * @param fParent           Database column f_parent SqlType(varchar)
    * @param fName             Database column f_name SqlType(varchar)
    * @param fExtension        Database column f_extension SqlType(varchar), Default(None)
    * @param fCreationDate     Database column f_creation_date SqlType(timestamp)
    * @param fLastModifiedDate Database column f_last_modified_date SqlType(timestamp)
    * @param fSize             Database column f_size SqlType(int8), Default(None)
    * @param fMimeTypeJava     Database column f_mime_type_java SqlType(varchar), Default(None)
    * @param sha256            Database column sha256 SqlType(varchar), Default(None)
    * @param fNameLc           Database column f_name_lc SqlType(varchar) */
  case class SmFileCardRow(id: String, storeName: String, fParent: String, fName: String, fExtension: Option[String] = None, fCreationDate: java.time.LocalDateTime, fLastModifiedDate: java.time.LocalDateTime, fSize: Option[Long] = None, fMimeTypeJava: Option[String] = None, sha256: Option[String] = None, fNameLc: String)

  /** GetResult implicit for fetching SmFileCardRow objects using plain SQL queries */
  implicit def GetResultSmFileCardRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[java.time.LocalDateTime], e3: GR[Option[Long]]): GR[SmFileCardRow] = GR {
    prs =>
      import prs._
      SmFileCardRow.tupled((<<[String], <<[String], <<[String], <<[String], <<?[String], <<[java.time.LocalDateTime], <<[java.time.LocalDateTime], <<?[Long], <<?[String], <<?[String], <<[String]))
  }

  /** Table description of table sm_file_card. Objects of this class serve as prototypes for rows in queries. */
  class SmFileCard(_tableTag: Tag) extends profile.api.Table[SmFileCardRow](_tableTag, "sm_file_card") {
    def * = (id, storeName, fParent, fName, fExtension, fCreationDate, fLastModifiedDate, fSize, fMimeTypeJava, sha256, fNameLc) <> (SmFileCardRow.tupled, SmFileCardRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(storeName), Rep.Some(fParent), Rep.Some(fName), fExtension, Rep.Some(fCreationDate), Rep.Some(fLastModifiedDate), fSize, fMimeTypeJava, sha256, Rep.Some(fNameLc))).shaped.<>({ r => import r._; _1.map(_ => SmFileCardRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8, _9, _10, _11.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(varchar), PrimaryKey */
    val id: Rep[String] = column[String]("id", O.PrimaryKey)
    /** Database column store_name SqlType(varchar) */
    val storeName: Rep[String] = column[String]("store_name")
    /** Database column f_parent SqlType(varchar) */
    val fParent: Rep[String] = column[String]("f_parent")
    /** Database column f_name SqlType(varchar) */
    val fName: Rep[String] = column[String]("f_name")
    /** Database column f_extension SqlType(varchar), Default(None) */
    val fExtension: Rep[Option[String]] = column[Option[String]]("f_extension", O.Default(None))
    /** Database column f_creation_date SqlType(timestamp) */
    val fCreationDate: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("f_creation_date")
    /** Database column f_last_modified_date SqlType(timestamp) */
    val fLastModifiedDate: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("f_last_modified_date")
    /** Database column f_size SqlType(int8), Default(None) */
    val fSize: Rep[Option[Long]] = column[Option[Long]]("f_size", O.Default(None))
    /** Database column f_mime_type_java SqlType(varchar), Default(None) */
    val fMimeTypeJava: Rep[Option[String]] = column[Option[String]]("f_mime_type_java", O.Default(None))
    /** Database column sha256 SqlType(varchar), Default(None) */
    val sha256: Rep[Option[String]] = column[Option[String]]("sha256", O.Default(None))
    /** Database column f_name_lc SqlType(varchar) */
    val fNameLc: Rep[String] = column[String]("f_name_lc")

    /** Index over (fParent) (database name f_parent_idx) */
    val index1 = index("f_parent_idx", fParent)
    /** Index over (fLastModifiedDate) (database name last_modified_idx) */
    val index2 = index("last_modified_idx", fLastModifiedDate)
    /** Index over (sha256) (database name sha256_idx) */
    val index3 = index("sha256_idx", sha256)
  }

  /** Collection-like TableQuery object for table SmFileCard */
  lazy val SmFileCard = new TableQuery(tag => new SmFileCard(tag))

  /** Entity class storing rows of table SmPathMove
    *
    * @param id        Database column id SqlType(serial), AutoInc, PrimaryKey
    * @param storeName Database column store_name SqlType(varchar)
    * @param pathFrom  Database column path_from SqlType(varchar)
    * @param pathTo    Database column path_to SqlType(varchar) */
  case class SmPathMoveRow(id: Int, storeName: String, pathFrom: String, pathTo: String)

  /** GetResult implicit for fetching SmPathMoveRow objects using plain SQL queries */
  implicit def GetResultSmPathMoveRow(implicit e0: GR[Int], e1: GR[String]): GR[SmPathMoveRow] = GR {
    prs =>
      import prs._
      SmPathMoveRow.tupled((<<[Int], <<[String], <<[String], <<[String]))
  }

  /** Table description of table sm_path_move. Objects of this class serve as prototypes for rows in queries. */
  class SmPathMove(_tableTag: Tag) extends profile.api.Table[SmPathMoveRow](_tableTag, "sm_path_move") {
    def * = (id, storeName, pathFrom, pathTo) <> (SmPathMoveRow.tupled, SmPathMoveRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(storeName), Rep.Some(pathFrom), Rep.Some(pathTo))).shaped.<>({ r => import r._; _1.map(_ => SmPathMoveRow.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column store_name SqlType(varchar) */
    val storeName: Rep[String] = column[String]("store_name")
    /** Database column path_from SqlType(varchar) */
    val pathFrom: Rep[String] = column[String]("path_from")
    /** Database column path_to SqlType(varchar) */
    val pathTo: Rep[String] = column[String]("path_to")
  }

  /** Collection-like TableQuery object for table SmPathMove */
  lazy val SmPathMove = new TableQuery(tag => new SmPathMove(tag))
}

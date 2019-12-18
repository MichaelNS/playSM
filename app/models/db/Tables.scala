package models.db

// AUTO-GENERATED Slick data model [2019-12-25T19:05:31.299+03:00[Europe/Moscow]]

/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = utils.db.SmPostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: utils.db.SmPostgresDriver
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(SmCategoryFc.schema, SmCategoryRule.schema, SmDevice.schema, SmDeviceScan.schema, SmExif.schema, SmFileCard.schema, SmImageResize.schema, SmJobPathMove.schema, SmLog.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table SmCategoryFc
   *  @param id Database column id SqlType(varchar)
   *  @param fName Database column f_name SqlType(varchar)
   *  @param categoryType Database column category_type SqlType(varchar), Default(None)
   *  @param category Database column category SqlType(varchar), Default(None)
   *  @param subCategory Database column sub_category SqlType(varchar), Default(None)
   *  @param description Database column description SqlType(varchar), Default(None) */
  case class SmCategoryFcRow(id: String, fName: String, categoryType: Option[String] = None, category: Option[String] = None, subCategory: Option[String] = None, description: Option[String] = None)
  /** GetResult implicit for fetching SmCategoryFcRow objects using plain SQL queries */
  implicit def GetResultSmCategoryFcRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[SmCategoryFcRow] = GR{
    prs => import prs._
    SmCategoryFcRow.tupled((<<[String], <<[String], <<?[String], <<?[String], <<?[String], <<?[String]))
  }
  /** Table description of table sm_category_fc. Objects of this class serve as prototypes for rows in queries. */
  class SmCategoryFc(_tableTag: Tag) extends profile.api.Table[SmCategoryFcRow](_tableTag, "sm_category_fc") {
    def * = (id, fName, categoryType, category, subCategory, description) <> (SmCategoryFcRow.tupled, SmCategoryFcRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(fName), categoryType, category, subCategory, description)).shaped.<>({r=>import r._; _1.map(_=> SmCategoryFcRow.tupled((_1.get, _2.get, _3, _4, _5, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(varchar) */
    val id: Rep[String] = column[String]("id")
    /** Database column f_name SqlType(varchar) */
    val fName: Rep[String] = column[String]("f_name")
    /** Database column category_type SqlType(varchar), Default(None) */
    val categoryType: Rep[Option[String]] = column[Option[String]]("category_type", O.Default(None))
    /** Database column category SqlType(varchar), Default(None) */
    val category: Rep[Option[String]] = column[Option[String]]("category", O.Default(None))
    /** Database column sub_category SqlType(varchar), Default(None) */
    val subCategory: Rep[Option[String]] = column[Option[String]]("sub_category", O.Default(None))
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))

    /** Primary key of SmCategoryFc (database name sm_category_fc_pkey) */
    val pk = primaryKey("sm_category_fc_pkey", (id, fName))

    /** Index over (categoryType,category,subCategory) (database name idx_sm_category_fc_category_type) */
    val index1 = index("idx_sm_category_fc_category_type", (categoryType, category, subCategory))
  }
  /** Collection-like TableQuery object for table SmCategoryFc */
  lazy val SmCategoryFc = new TableQuery(tag => new SmCategoryFc(tag))

  /** Entity class storing rows of table SmCategoryRule
   *  @param categoryType Database column category_type SqlType(varchar)
   *  @param category Database column category SqlType(varchar)
   *  @param subCategory Database column sub_category SqlType(varchar)
   *  @param fPath Database column f_path SqlType(varchar)
   *  @param isBegins Database column is_begins SqlType(bool)
   *  @param description Database column description SqlType(varchar) */
  case class SmCategoryRuleRow(categoryType: String, category: String, subCategory: String, fPath: String, isBegins: Boolean, description: String)
  /** GetResult implicit for fetching SmCategoryRuleRow objects using plain SQL queries */
  implicit def GetResultSmCategoryRuleRow(implicit e0: GR[String], e1: GR[Boolean]): GR[SmCategoryRuleRow] = GR{
    prs => import prs._
    SmCategoryRuleRow.tupled((<<[String], <<[String], <<[String], <<[String], <<[Boolean], <<[String]))
  }
  /** Table description of table sm_category_rule. Objects of this class serve as prototypes for rows in queries. */
  class SmCategoryRule(_tableTag: Tag) extends profile.api.Table[SmCategoryRuleRow](_tableTag, "sm_category_rule") {
    def * = (categoryType, category, subCategory, fPath, isBegins, description) <> (SmCategoryRuleRow.tupled, SmCategoryRuleRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(categoryType), Rep.Some(category), Rep.Some(subCategory), Rep.Some(fPath), Rep.Some(isBegins), Rep.Some(description))).shaped.<>({r=>import r._; _1.map(_=> SmCategoryRuleRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column category_type SqlType(varchar) */
    val categoryType: Rep[String] = column[String]("category_type")
    /** Database column category SqlType(varchar) */
    val category: Rep[String] = column[String]("category")
    /** Database column sub_category SqlType(varchar) */
    val subCategory: Rep[String] = column[String]("sub_category")
    /** Database column f_path SqlType(varchar) */
    val fPath: Rep[String] = column[String]("f_path")
    /** Database column is_begins SqlType(bool) */
    val isBegins: Rep[Boolean] = column[Boolean]("is_begins")
    /** Database column description SqlType(varchar) */
    val description: Rep[String] = column[String]("description")

    /** Primary key of SmCategoryRule (database name sm_category_rule_pkey) */
    val pk = primaryKey("sm_category_rule_pkey", (categoryType, category, subCategory))
  }
  /** Collection-like TableQuery object for table SmCategoryRule */
  lazy val SmCategoryRule = new TableQuery(tag => new SmCategoryRule(tag))

  /** Entity class storing rows of table SmDevice
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param uid Database column uid SqlType(varchar)
   *  @param name Database column name SqlType(varchar)
   *  @param labelV Database column label_v SqlType(varchar)
   *  @param nameV Database column name_v SqlType(varchar), Default(None)
   *  @param description Database column description SqlType(varchar), Default(None)
   *  @param visible Database column visible SqlType(bool), Default(true)
   *  @param reliable Database column reliable SqlType(bool), Default(true)
   *  @param pathScanDate Database column path_scan_date SqlType(timestamp)
   *  @param crcDate Database column crc_date SqlType(timestamp), Default(None)
   *  @param exifDate Database column exif_date SqlType(timestamp), Default(None)
   *  @param jobPathScan Database column job_path_scan SqlType(bool), Default(Some(false))
   *  @param jobCalcCrc Database column job_calc_crc SqlType(bool), Default(Some(false))
   *  @param jobCalcExif Database column job_calc_exif SqlType(bool), Default(Some(false))
   *  @param jobResize Database column job_resize SqlType(bool), Default(Some(false)) */
  case class SmDeviceRow(id: Int, uid: String, name: String, labelV: String, nameV: Option[String] = None, description: Option[String] = None, visible: Boolean = true, reliable: Boolean = true, pathScanDate: java.time.LocalDateTime, crcDate: Option[java.time.LocalDateTime] = None, exifDate: Option[java.time.LocalDateTime] = None, jobPathScan: Option[Boolean] = Some(false), jobCalcCrc: Option[Boolean] = Some(false), jobCalcExif: Option[Boolean] = Some(false), jobResize: Option[Boolean] = Some(false))
  /** GetResult implicit for fetching SmDeviceRow objects using plain SQL queries */
  implicit def GetResultSmDeviceRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]], e3: GR[Boolean], e4: GR[java.time.LocalDateTime], e5: GR[Option[java.time.LocalDateTime]], e6: GR[Option[Boolean]]): GR[SmDeviceRow] = GR{
    prs => import prs._
    SmDeviceRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<?[String], <<?[String], <<[Boolean], <<[Boolean], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<?[Boolean], <<?[Boolean], <<?[Boolean], <<?[Boolean]))
  }
  /** Table description of table sm_device. Objects of this class serve as prototypes for rows in queries. */
  class SmDevice(_tableTag: Tag) extends profile.api.Table[SmDeviceRow](_tableTag, "sm_device") {
    def * = (id, uid, name, labelV, nameV, description, visible, reliable, pathScanDate, crcDate, exifDate, jobPathScan, jobCalcCrc, jobCalcExif, jobResize) <> (SmDeviceRow.tupled, SmDeviceRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(uid), Rep.Some(name), Rep.Some(labelV), nameV, description, Rep.Some(visible), Rep.Some(reliable), Rep.Some(pathScanDate), crcDate, exifDate, jobPathScan, jobCalcCrc, jobCalcExif, jobResize)).shaped.<>({r=>import r._; _1.map(_=> SmDeviceRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6, _7.get, _8.get, _9.get, _10, _11, _12, _13, _14, _15)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column uid SqlType(varchar) */
    val uid: Rep[String] = column[String]("uid")
    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column label_v SqlType(varchar) */
    val labelV: Rep[String] = column[String]("label_v")
    /** Database column name_v SqlType(varchar), Default(None) */
    val nameV: Rep[Option[String]] = column[Option[String]]("name_v", O.Default(None))
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column visible SqlType(bool), Default(true) */
    val visible: Rep[Boolean] = column[Boolean]("visible", O.Default(true))
    /** Database column reliable SqlType(bool), Default(true) */
    val reliable: Rep[Boolean] = column[Boolean]("reliable", O.Default(true))
    /** Database column path_scan_date SqlType(timestamp) */
    val pathScanDate: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("path_scan_date")
    /** Database column crc_date SqlType(timestamp), Default(None) */
    val crcDate: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("crc_date", O.Default(None))
    /** Database column exif_date SqlType(timestamp), Default(None) */
    val exifDate: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("exif_date", O.Default(None))
    /** Database column job_path_scan SqlType(bool), Default(Some(false)) */
    val jobPathScan: Rep[Option[Boolean]] = column[Option[Boolean]]("job_path_scan", O.Default(Some(false)))
    /** Database column job_calc_crc SqlType(bool), Default(Some(false)) */
    val jobCalcCrc: Rep[Option[Boolean]] = column[Option[Boolean]]("job_calc_crc", O.Default(Some(false)))
    /** Database column job_calc_exif SqlType(bool), Default(Some(false)) */
    val jobCalcExif: Rep[Option[Boolean]] = column[Option[Boolean]]("job_calc_exif", O.Default(Some(false)))
    /** Database column job_resize SqlType(bool), Default(Some(false)) */
    val jobResize: Rep[Option[Boolean]] = column[Option[Boolean]]("job_resize", O.Default(Some(false)))

    /** Uniqueness Index over (uid) (database name idx_sm_device_device_uid) */
    val index1 = index("idx_sm_device_device_uid", uid, unique=true)
  }
  /** Collection-like TableQuery object for table SmDevice */
  lazy val SmDevice = new TableQuery(tag => new SmDevice(tag))

  /** Entity class storing rows of table SmDeviceScan
   *  @param deviceUid Database column device_uid SqlType(varchar)
   *  @param fPath Database column f_path SqlType(varchar) */
  case class SmDeviceScanRow(deviceUid: String, fPath: String)
  /** GetResult implicit for fetching SmDeviceScanRow objects using plain SQL queries */
  implicit def GetResultSmDeviceScanRow(implicit e0: GR[String]): GR[SmDeviceScanRow] = GR{
    prs => import prs._
    SmDeviceScanRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table sm_device_scan. Objects of this class serve as prototypes for rows in queries. */
  class SmDeviceScan(_tableTag: Tag) extends profile.api.Table[SmDeviceScanRow](_tableTag, "sm_device_scan") {
    def * = (deviceUid, fPath) <> (SmDeviceScanRow.tupled, SmDeviceScanRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(deviceUid), Rep.Some(fPath))).shaped.<>({r=>import r._; _1.map(_=> SmDeviceScanRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column device_uid SqlType(varchar) */
    val deviceUid: Rep[String] = column[String]("device_uid")
    /** Database column f_path SqlType(varchar) */
    val fPath: Rep[String] = column[String]("f_path")

    /** Foreign key referencing SmDevice (database name fk_sm_device_scan_sm_device) */
    lazy val smDeviceFk = foreignKey("fk_sm_device_scan_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)

    /** Uniqueness Index over (deviceUid,fPath) (database name idx_sm_device_scan_device_uid) */
    val index1 = index("idx_sm_device_scan_device_uid", (deviceUid, fPath), unique=true)
  }
  /** Collection-like TableQuery object for table SmDeviceScan */
  lazy val SmDeviceScan = new TableQuery(tag => new SmDeviceScan(tag))

  /** Entity class storing rows of table SmExif
   *  @param id Database column id SqlType(varchar), PrimaryKey
   *  @param dateTime Database column date_time SqlType(timestamp), Default(None)
   *  @param dateTimeOriginal Database column date_time_original SqlType(timestamp), Default(None)
   *  @param dateTimeDigitized Database column date_time_digitized SqlType(timestamp), Default(None)
   *  @param make Database column make SqlType(varchar), Default(None)
   *  @param model Database column model SqlType(varchar), Default(None)
   *  @param software Database column software SqlType(varchar), Default(None)
   *  @param exifImageWidth Database column exif_image_width SqlType(varchar), Default(None)
   *  @param exifImageHeight Database column exif_image_height SqlType(varchar), Default(None)
   *  @param gpsVersionId Database column gps_version_id SqlType(varchar), Default(None)
   *  @param gpsLatitudeRef Database column gps_latitude_ref SqlType(varchar), Default(None)
   *  @param gpsLatitude Database column gps_latitude SqlType(varchar), Default(None)
   *  @param gpsLongitudeRef Database column gps_longitude_ref SqlType(varchar), Default(None)
   *  @param gpsLongitude Database column gps_longitude SqlType(varchar), Default(None)
   *  @param gpsAltitudeRef Database column gps_altitude_ref SqlType(varchar), Default(None)
   *  @param gpsAltitude Database column gps_altitude SqlType(varchar), Default(None)
   *  @param gpsTimeStamp Database column gps_time_stamp SqlType(varchar), Default(None)
   *  @param gpsProcessingMethod Database column gps_processing_method SqlType(varchar), Default(None)
   *  @param gpsDateStamp Database column gps_date_stamp SqlType(varchar), Default(None)
   *  @param gpsLatitudeDec Database column gps_latitude_dec SqlType(numeric), Default(None)
   *  @param gpsLongitudeDec Database column gps_longitude_dec SqlType(numeric), Default(None) */
  case class SmExifRow(id: String, dateTime: Option[java.time.LocalDateTime] = None, dateTimeOriginal: Option[java.time.LocalDateTime] = None, dateTimeDigitized: Option[java.time.LocalDateTime] = None, make: Option[String] = None, model: Option[String] = None, software: Option[String] = None, exifImageWidth: Option[String] = None, exifImageHeight: Option[String] = None, gpsVersionId: Option[String] = None, gpsLatitudeRef: Option[String] = None, gpsLatitude: Option[String] = None, gpsLongitudeRef: Option[String] = None, gpsLongitude: Option[String] = None, gpsAltitudeRef: Option[String] = None, gpsAltitude: Option[String] = None, gpsTimeStamp: Option[String] = None, gpsProcessingMethod: Option[String] = None, gpsDateStamp: Option[String] = None, gpsLatitudeDec: Option[scala.math.BigDecimal] = None, gpsLongitudeDec: Option[scala.math.BigDecimal] = None)
  /** GetResult implicit for fetching SmExifRow objects using plain SQL queries */
  implicit def GetResultSmExifRow(implicit e0: GR[String], e1: GR[Option[java.time.LocalDateTime]], e2: GR[Option[String]], e3: GR[Option[scala.math.BigDecimal]]): GR[SmExifRow] = GR{
    prs => import prs._
    SmExifRow.tupled((<<[String], <<?[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }
  /** Table description of table sm_exif. Objects of this class serve as prototypes for rows in queries. */
  class SmExif(_tableTag: Tag) extends profile.api.Table[SmExifRow](_tableTag, "sm_exif") {
    def * = (id, dateTime, dateTimeOriginal, dateTimeDigitized, make, model, software, exifImageWidth, exifImageHeight, gpsVersionId, gpsLatitudeRef, gpsLatitude, gpsLongitudeRef, gpsLongitude, gpsAltitudeRef, gpsAltitude, gpsTimeStamp, gpsProcessingMethod, gpsDateStamp, gpsLatitudeDec, gpsLongitudeDec) <> (SmExifRow.tupled, SmExifRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), dateTime, dateTimeOriginal, dateTimeDigitized, make, model, software, exifImageWidth, exifImageHeight, gpsVersionId, gpsLatitudeRef, gpsLatitude, gpsLongitudeRef, gpsLongitude, gpsAltitudeRef, gpsAltitude, gpsTimeStamp, gpsProcessingMethod, gpsDateStamp, gpsLatitudeDec, gpsLongitudeDec)).shaped.<>({r=>import r._; _1.map(_=> SmExifRow.tupled((_1.get, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20, _21)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

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
    lazy val smFileCardFk = foreignKey("fk_sm_exif_sm_file_card", id, SmFileCard)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table SmExif */
  lazy val SmExif = new TableQuery(tag => new SmExif(tag))

  /** Entity class storing rows of table SmFileCard
   *  @param id Database column id SqlType(varchar), PrimaryKey
   *  @param deviceUid Database column device_uid SqlType(varchar)
   *  @param fParent Database column f_parent SqlType(varchar)
   *  @param fName Database column f_name SqlType(varchar)
   *  @param fExtension Database column f_extension SqlType(varchar), Default(None)
   *  @param fCreationDate Database column f_creation_date SqlType(timestamp)
   *  @param fLastModifiedDate Database column f_last_modified_date SqlType(timestamp)
   *  @param fSize Database column f_size SqlType(int8), Default(None)
   *  @param fMimeTypeJava Database column f_mime_type_java SqlType(varchar), Default(None)
   *  @param sha256 Database column sha256 SqlType(varchar), Default(None)
   *  @param fNameLc Database column f_name_lc SqlType(varchar) */
  case class SmFileCardRow(id: String, deviceUid: String, fParent: String, fName: String, fExtension: Option[String] = None, fCreationDate: java.time.LocalDateTime, fLastModifiedDate: java.time.LocalDateTime, fSize: Option[Long] = None, fMimeTypeJava: Option[String] = None, sha256: Option[String] = None, fNameLc: String)
  /** GetResult implicit for fetching SmFileCardRow objects using plain SQL queries */
  implicit def GetResultSmFileCardRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[java.time.LocalDateTime], e3: GR[Option[Long]]): GR[SmFileCardRow] = GR{
    prs => import prs._
    SmFileCardRow.tupled((<<[String], <<[String], <<[String], <<[String], <<?[String], <<[java.time.LocalDateTime], <<[java.time.LocalDateTime], <<?[Long], <<?[String], <<?[String], <<[String]))
  }
  /** Table description of table sm_file_card. Objects of this class serve as prototypes for rows in queries. */
  class SmFileCard(_tableTag: Tag) extends profile.api.Table[SmFileCardRow](_tableTag, "sm_file_card") {
    def * = (id, deviceUid, fParent, fName, fExtension, fCreationDate, fLastModifiedDate, fSize, fMimeTypeJava, sha256, fNameLc) <> (SmFileCardRow.tupled, SmFileCardRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(deviceUid), Rep.Some(fParent), Rep.Some(fName), fExtension, Rep.Some(fCreationDate), Rep.Some(fLastModifiedDate), fSize, fMimeTypeJava, sha256, Rep.Some(fNameLc))).shaped.<>({r=>import r._; _1.map(_=> SmFileCardRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8, _9, _10, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(varchar), PrimaryKey */
    val id: Rep[String] = column[String]("id", O.PrimaryKey)
    /** Database column device_uid SqlType(varchar) */
    val deviceUid: Rep[String] = column[String]("device_uid")
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

    /** Foreign key referencing SmDevice (database name fk_sm_file_card_sm_device) */
    lazy val smDeviceFk = foreignKey("fk_sm_file_card_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Restrict)

    /** Index over (fParent) (database name idx_f_parent) */
    val index1 = index("idx_f_parent", fParent)
    /** Index over (fNameLc) (database name idx_fc_f_name_lc) */
    val index2 = index("idx_fc_f_name_lc", fNameLc)
    /** Index over (sha256,fName) (database name idx_fc_sha_name) */
    val index3 = index("idx_fc_sha_name", (sha256, fName))
    /** Index over (fLastModifiedDate) (database name idx_last_modified) */
    val index4 = index("idx_last_modified", fLastModifiedDate)
    /** Index over (sha256) (database name idx_sha256) */
    val index5 = index("idx_sha256", sha256)
    /** Index over (deviceUid,fParent) (database name idx_sm_file_card_device_uid) */
    val index6 = index("idx_sm_file_card_device_uid", (deviceUid, fParent))
  }
  /** Collection-like TableQuery object for table SmFileCard */
  lazy val SmFileCard = new TableQuery(tag => new SmFileCard(tag))

  /** Entity class storing rows of table SmImageResize
   *  @param id Database column id SqlType(varchar), PrimaryKey
   *  @param imageName Database column image_name SqlType(varchar)
   *  @param imagePath Database column image_path SqlType(varchar) */
  case class SmImageResizeRow(id: String, imageName: String, imagePath: String)
  /** GetResult implicit for fetching SmImageResizeRow objects using plain SQL queries */
  implicit def GetResultSmImageResizeRow(implicit e0: GR[String]): GR[SmImageResizeRow] = GR{
    prs => import prs._
    SmImageResizeRow.tupled((<<[String], <<[String], <<[String]))
  }
  /** Table description of table sm_image_resize. Objects of this class serve as prototypes for rows in queries. */
  class SmImageResize(_tableTag: Tag) extends profile.api.Table[SmImageResizeRow](_tableTag, "sm_image_resize") {
    def * = (id, imageName, imagePath) <> (SmImageResizeRow.tupled, SmImageResizeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(imageName), Rep.Some(imagePath))).shaped.<>({r=>import r._; _1.map(_=> SmImageResizeRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(varchar), PrimaryKey */
    val id: Rep[String] = column[String]("id", O.PrimaryKey)
    /** Database column image_name SqlType(varchar) */
    val imageName: Rep[String] = column[String]("image_name")
    /** Database column image_path SqlType(varchar) */
    val imagePath: Rep[String] = column[String]("image_path")

    /** Foreign key referencing SmFileCard (database name fk_sm_image_resize_sm_file_card) */
    lazy val smFileCardFk = foreignKey("fk_sm_image_resize_sm_file_card", id, SmFileCard)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table SmImageResize */
  lazy val SmImageResize = new TableQuery(tag => new SmImageResize(tag))

  /** Entity class storing rows of table SmJobPathMove
   *  @param id Database column id SqlType(serial), AutoInc
   *  @param deviceUid Database column device_uid SqlType(varchar)
   *  @param pathFrom Database column path_from SqlType(varchar)
   *  @param pathTo Database column path_to SqlType(varchar)
   *  @param done Database column done SqlType(timestamp), Default(None) */
  case class SmJobPathMoveRow(id: Int, deviceUid: String, pathFrom: String, pathTo: String, done: Option[java.time.LocalDateTime] = None)
  /** GetResult implicit for fetching SmJobPathMoveRow objects using plain SQL queries */
  implicit def GetResultSmJobPathMoveRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[java.time.LocalDateTime]]): GR[SmJobPathMoveRow] = GR{
    prs => import prs._
    SmJobPathMoveRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<?[java.time.LocalDateTime]))
  }
  /** Table description of table sm_job_path_move. Objects of this class serve as prototypes for rows in queries. */
  class SmJobPathMove(_tableTag: Tag) extends profile.api.Table[SmJobPathMoveRow](_tableTag, "sm_job_path_move") {
    def * = (id, deviceUid, pathFrom, pathTo, done) <> (SmJobPathMoveRow.tupled, SmJobPathMoveRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(deviceUid), Rep.Some(pathFrom), Rep.Some(pathTo), done)).shaped.<>({r=>import r._; _1.map(_=> SmJobPathMoveRow.tupled((_1.get, _2.get, _3.get, _4.get, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc */
    val id: Rep[Int] = column[Int]("id", O.AutoInc)
    /** Database column device_uid SqlType(varchar) */
    val deviceUid: Rep[String] = column[String]("device_uid")
    /** Database column path_from SqlType(varchar) */
    val pathFrom: Rep[String] = column[String]("path_from")
    /** Database column path_to SqlType(varchar) */
    val pathTo: Rep[String] = column[String]("path_to")
    /** Database column done SqlType(timestamp), Default(None) */
    val done: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("done", O.Default(None))

    /** Primary key of SmJobPathMove (database name idx_sm_job_path_move_device_uid) */
    val pk = primaryKey("idx_sm_job_path_move_device_uid", (deviceUid, pathFrom))

    /** Foreign key referencing SmDevice (database name fk_sm_job_path_move_sm_device) */
    lazy val smDeviceFk = foreignKey("fk_sm_job_path_move_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)

    /** Uniqueness Index over (id) (database name unq_sm_job_path_move) */
    val index1 = index("unq_sm_job_path_move", id, unique=true)
  }
  /** Collection-like TableQuery object for table SmJobPathMove */
  lazy val SmJobPathMove = new TableQuery(tag => new SmJobPathMove(tag))

  /** Entity class storing rows of table SmLog
   *  @param createDate Database column create_date SqlType(timestamp)
   *  @param deviceUid Database column device_uid SqlType(varchar)
   *  @param level Database column level SqlType(varchar)
   *  @param step Database column step SqlType(varchar)
   *  @param error Database column error SqlType(varchar)
   *  @param stackTrace Database column stack_trace SqlType(varchar), Default(None) */
  case class SmLogRow(createDate: Option[java.time.LocalDateTime], deviceUid: String, level: String, step: String, error: String, stackTrace: Option[String] = None)
  /** GetResult implicit for fetching SmLogRow objects using plain SQL queries */
  implicit def GetResultSmLogRow(implicit e0: GR[Option[java.time.LocalDateTime]], e1: GR[String], e2: GR[Option[String]]): GR[SmLogRow] = GR{
    prs => import prs._
    SmLogRow.tupled((<<?[java.time.LocalDateTime], <<[String], <<[String], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table sm_log. Objects of this class serve as prototypes for rows in queries. */
  class SmLog(_tableTag: Tag) extends profile.api.Table[SmLogRow](_tableTag, "sm_log") {
    def * = (createDate, deviceUid, level, step, error, stackTrace) <> (SmLogRow.tupled, SmLogRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((createDate, Rep.Some(deviceUid), Rep.Some(level), Rep.Some(step), Rep.Some(error), stackTrace)).shaped.<>({r=>import r._; _2.map(_=> SmLogRow.tupled((_1, _2.get, _3.get, _4.get, _5.get, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column create_date SqlType(timestamp) */
    val createDate: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("create_date")
    /** Database column device_uid SqlType(varchar) */
    val deviceUid: Rep[String] = column[String]("device_uid")
    /** Database column level SqlType(varchar) */
    val level: Rep[String] = column[String]("level")
    /** Database column step SqlType(varchar) */
    val step: Rep[String] = column[String]("step")
    /** Database column error SqlType(varchar) */
    val error: Rep[String] = column[String]("error")
    /** Database column stack_trace SqlType(varchar), Default(None) */
    val stackTrace: Rep[Option[String]] = column[Option[String]]("stack_trace", O.Default(None))

    /** Foreign key referencing SmDevice (database name fk_sm_log_sm_device) */
    lazy val smDeviceFk = foreignKey("fk_sm_log_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table SmLog */
  lazy val SmLog = new TableQuery(tag => new SmLog(tag))
}

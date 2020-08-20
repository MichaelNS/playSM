package models.db

// AUTO-GENERATED Slick data model for table SmDevice
trait SmDeviceTable {

  self: Tables =>

  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmDevice
    *
    * @param id           Database column id SqlType(serial), AutoInc, PrimaryKey
    * @param uid          Database column uid SqlType(varchar)
    * @param name         Database column name SqlType(varchar)
    * @param labelV       Database column label_v SqlType(varchar)
    * @param nameV        Database column name_v SqlType(varchar), Default(None)
    * @param description  Database column description SqlType(varchar), Default(None)
    * @param visible      Database column visible SqlType(bool), Default(true)
    * @param reliable     Database column reliable SqlType(bool), Default(true)
    * @param pathScanDate Database column path_scan_date SqlType(timestamp), Default(None)
    * @param crcDate      Database column crc_date SqlType(timestamp), Default(None)
    * @param exifDate     Database column exif_date SqlType(timestamp), Default(None)
    * @param jobPathScan  Database column job_path_scan SqlType(bool), Default(false)
    * @param jobCalcCrc   Database column job_calc_crc SqlType(bool), Default(false)
    * @param jobCalcExif  Database column job_calc_exif SqlType(bool), Default(false)
    * @param jobResize    Database column job_resize SqlType(bool), Default(false) */
  case class SmDeviceRow(id: Int, uid: String, name: String, labelV: String, nameV: Option[String] = None, description: Option[String] = None, visible: Boolean = true, reliable: Boolean = true, pathScanDate: Option[java.time.LocalDateTime] = None, crcDate: Option[java.time.LocalDateTime] = None, exifDate: Option[java.time.LocalDateTime] = None, jobPathScan: Boolean = false, jobCalcCrc: Boolean = false, jobCalcExif: Boolean = false, jobResize: Boolean = false)

  /** GetResult implicit for fetching SmDeviceRow objects using plain SQL queries */
  implicit def GetResultSmDeviceRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]], e3: GR[Boolean], e4: GR[Option[java.time.LocalDateTime]]): GR[SmDeviceRow] = GR {
    prs =>
      import prs._
      SmDeviceRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<?[String], <<?[String], <<[Boolean], <<[Boolean], <<?[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<?[java.time.LocalDateTime], <<[Boolean], <<[Boolean], <<[Boolean], <<[Boolean]))
  }

  /** Table description of table sm_device. Objects of this class serve as prototypes for rows in queries. */
  class SmDevice(_tableTag: Tag) extends profile.api.Table[SmDeviceRow](_tableTag, "sm_device") {
    def * = (id, uid, name, labelV, nameV, description, visible, reliable, pathScanDate, crcDate, exifDate, jobPathScan, jobCalcCrc, jobCalcExif, jobResize) <> (SmDeviceRow.tupled, SmDeviceRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(uid), Rep.Some(name), Rep.Some(labelV), nameV, description, Rep.Some(visible), Rep.Some(reliable), pathScanDate, crcDate, exifDate, Rep.Some(jobPathScan), Rep.Some(jobCalcCrc), Rep.Some(jobCalcExif), Rep.Some(jobResize)).shaped.<>({ r => import r._; _1.map(_ => SmDeviceRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6, _7.get, _8.get, _9, _10, _11, _12.get, _13.get, _14.get, _15.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

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
    /** Database column path_scan_date SqlType(timestamp), Default(None) */
    val pathScanDate: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("path_scan_date", O.Default(None))
    /** Database column crc_date SqlType(timestamp), Default(None) */
    val crcDate: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("crc_date", O.Default(None))
    /** Database column exif_date SqlType(timestamp), Default(None) */
    val exifDate: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("exif_date", O.Default(None))
    /** Database column job_path_scan SqlType(bool), Default(false) */
    val jobPathScan: Rep[Boolean] = column[Boolean]("job_path_scan", O.Default(false))
    /** Database column job_calc_crc SqlType(bool), Default(false) */
    val jobCalcCrc: Rep[Boolean] = column[Boolean]("job_calc_crc", O.Default(false))
    /** Database column job_calc_exif SqlType(bool), Default(false) */
    val jobCalcExif: Rep[Boolean] = column[Boolean]("job_calc_exif", O.Default(false))
    /** Database column job_resize SqlType(bool), Default(false) */
    val jobResize: Rep[Boolean] = column[Boolean]("job_resize", O.Default(false))

    /** Uniqueness Index over (uid) (database name idx_sm_device_device_uid) */
    val index1 = index("idx_sm_device_device_uid", uid, unique = true)
  }

  /** Collection-like TableQuery object for table SmDevice */
  lazy val SmDevice = new TableQuery(tag => new SmDevice(tag))
}

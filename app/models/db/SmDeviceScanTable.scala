package models.db

// AUTO-GENERATED Slick data model for table SmDeviceScan
trait SmDeviceScanTable {

  self: Tables =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmDeviceScan
    *
    * @param deviceUid Database column device_uid SqlType(varchar)
    * @param fPath     Database column f_path SqlType(varchar) */
  case class SmDeviceScanRow(deviceUid: String, fPath: String)

  /** GetResult implicit for fetching SmDeviceScanRow objects using plain SQL queries */
  implicit def GetResultSmDeviceScanRow(implicit e0: GR[String]): GR[SmDeviceScanRow] = GR {
    prs =>
      import prs._
      SmDeviceScanRow.tupled((<<[String], <<[String]))
  }

  /** Table description of table sm_device_scan. Objects of this class serve as prototypes for rows in queries. */
  class SmDeviceScan(_tableTag: Tag) extends profile.api.Table[SmDeviceScanRow](_tableTag, "sm_device_scan") {
    def * = (deviceUid, fPath) <> (SmDeviceScanRow.tupled, SmDeviceScanRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(deviceUid), Rep.Some(fPath)).shaped.<>({ r => import r._; _1.map(_ => SmDeviceScanRow.tupled((_1.get, _2.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column device_uid SqlType(varchar) */
    val deviceUid: Rep[String] = column[String]("device_uid")
    /** Database column f_path SqlType(varchar) */
    val fPath: Rep[String] = column[String]("f_path")

    /** Foreign key referencing SmDevice (database name fk_sm_device_scan_sm_device) */
    lazy val smDeviceFk = foreignKey("fk_sm_device_scan_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (deviceUid,fPath) (database name idx_sm_device_scan_device_uid) */
    val index1 = index("idx_sm_device_scan_device_uid", (deviceUid, fPath), unique = true)
  }

  /** Collection-like TableQuery object for table SmDeviceScan */
  lazy val SmDeviceScan = new TableQuery(tag => new SmDeviceScan(tag))
}

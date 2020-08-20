package models.db

// AUTO-GENERATED Slick data model for table SmFileCard
trait SmFileCardTable {

  self: Tables =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmFileCard
    *
    * @param id                Database column id SqlType(varchar), PrimaryKey
    * @param deviceUid         Database column device_uid SqlType(varchar)
    * @param fParent           Database column f_parent SqlType(varchar)
    * @param fName             Database column f_name SqlType(varchar)
    * @param fExtension        Database column f_extension SqlType(varchar), Default(None)
    * @param fCreationDate     Database column f_creation_date SqlType(timestamp)
    * @param fLastModifiedDate Database column f_last_modified_date SqlType(timestamp)
    * @param fSize             Database column f_size SqlType(int8), Default(None)
    * @param fMimeTypeJava     Database column f_mime_type_java SqlType(varchar), Default(None)
    * @param sha256            Database column sha256 SqlType(varchar), Default(None)
    * @param fNameLc           Database column f_name_lc SqlType(varchar) */
  case class SmFileCardRow(id: String, deviceUid: String, fParent: String, fName: String, fExtension: Option[String] = None, fCreationDate: java.time.LocalDateTime, fLastModifiedDate: java.time.LocalDateTime, fSize: Option[Long] = None, fMimeTypeJava: Option[String] = None, sha256: Option[String] = None, fNameLc: String)

  /** GetResult implicit for fetching SmFileCardRow objects using plain SQL queries */
  implicit def GetResultSmFileCardRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[java.time.LocalDateTime], e3: GR[Option[Long]]): GR[SmFileCardRow] = GR {
    prs =>
      import prs._
      SmFileCardRow.tupled((<<[String], <<[String], <<[String], <<[String], <<?[String], <<[java.time.LocalDateTime], <<[java.time.LocalDateTime], <<?[Long], <<?[String], <<?[String], <<[String]))
  }

  /** Table description of table sm_file_card. Objects of this class serve as prototypes for rows in queries. */
  class SmFileCard(_tableTag: Tag) extends profile.api.Table[SmFileCardRow](_tableTag, "sm_file_card") {
    def * = (id, deviceUid, fParent, fName, fExtension, fCreationDate, fLastModifiedDate, fSize, fMimeTypeJava, sha256, fNameLc) <> (SmFileCardRow.tupled, SmFileCardRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(deviceUid), Rep.Some(fParent), Rep.Some(fName), fExtension, Rep.Some(fCreationDate), Rep.Some(fLastModifiedDate), fSize, fMimeTypeJava, sha256, Rep.Some(fNameLc)).shaped.<>({ r => import r._; _1.map(_ => SmFileCardRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8, _9, _10, _11.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

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
    lazy val smDeviceFk = foreignKey("fk_sm_file_card_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Restrict)

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
}

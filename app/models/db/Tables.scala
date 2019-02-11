package models.db

// AUTO-GENERATED Slick data model [2019-02-08T20:19:32.301+03:00[Europe/Moscow]]

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
  lazy val schema: profile.SchemaDescription = SmCategoryFc.schema ++ SmDevice.schema ++ SmFileCard.schema ++ SmPathMove.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl: profile.DDL = schema

  /** Entity class storing rows of table SmCategoryFc
    *
    * @param id              Database column ID SqlType(varchar)
    * @param fName           Database column F_NAME SqlType(varchar)
    * @param categoryType    Database column CATEGORY_TYPE SqlType(varchar), Default(None)
    * @param subCategoryType Database column SUB_CATEGORY_TYPE SqlType(varchar), Default(None)
    * @param description     Database column DESCRIPTION SqlType(varchar), Default(None) */
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
    def ? = (Rep.Some(id), Rep.Some(fName), categoryType, subCategoryType, description).shaped.<>({ r => import r._; _1.map(_ => SmCategoryFcRow.tupled((_1.get, _2.get, _3, _4, _5))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(varchar) */
    val id: Rep[String] = column[String]("ID")
    /** Database column F_NAME SqlType(varchar) */
    val fName: Rep[String] = column[String]("F_NAME")
    /** Database column CATEGORY_TYPE SqlType(varchar), Default(None) */
    val categoryType: Rep[Option[String]] = column[Option[String]]("CATEGORY_TYPE", O.Default(None))
    /** Database column SUB_CATEGORY_TYPE SqlType(varchar), Default(None) */
    val subCategoryType: Rep[Option[String]] = column[Option[String]]("SUB_CATEGORY_TYPE", O.Default(None))
    /** Database column DESCRIPTION SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Default(None))

    /** Primary key of SmCategoryFc (database name sm_category_fc_pkey) */
    val pk = primaryKey("sm_category_fc_pkey", (id, fName))
  }

  /** Collection-like TableQuery object for table SmCategoryFc */
  lazy val SmCategoryFc = new TableQuery(tag => new SmCategoryFc(tag))

  /** Entity class storing rows of table SmDevice
    *
    * @param id       Database column ID SqlType(serial), AutoInc, PrimaryKey
    * @param name     Database column NAME SqlType(varchar)
    * @param label    Database column LABEL SqlType(varchar)
    * @param uid      Database column UID SqlType(varchar)
    * @param syncDate Database column SYNC_DATE SqlType(timestamp)
    * @param describe Database column DESCRIBE SqlType(varchar), Default(None)
    * @param visible  Database column VISIBLE SqlType(bool), Default(true)
    * @param reliable Database column RELIABLE SqlType(bool), Default(true) */
  case class SmDeviceRow(id: Int,
                         name: String,
                         label: String,
                         uid: String,
                         syncDate: java.time.LocalDateTime,
                         describe: Option[String] = None,
                         visible: Boolean = true,
                         reliable: Boolean = true)

  /** GetResult implicit for fetching SmDeviceRow objects using plain SQL queries */
  implicit def GetResultSmDeviceRow(implicit e0: GR[Int],
                                    e1: GR[String],
                                    e2: GR[java.time.LocalDateTime],
                                    e3: GR[Option[String]],
                                    e4: GR[Boolean]): GR[SmDeviceRow] = GR {
    prs =>
      import prs._
      SmDeviceRow.tupled((<<[Int],
        <<[String],
        <<[String],
        <<[String],
        <<[java.time.LocalDateTime],
        <<?[String],
        <<[Boolean],
        <<[Boolean]))
  }

  /** Table description of table sm_device. Objects of this class serve as prototypes for rows in queries. */
  class SmDevice(_tableTag: Tag) extends profile.api.Table[SmDeviceRow](_tableTag, "sm_device") {
    def * = (id, name, label, uid, syncDate, describe, visible, reliable) <> (SmDeviceRow.tupled, SmDeviceRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(name), Rep.Some(label), Rep.Some(uid), Rep.Some(syncDate), describe, Rep.Some(visible), Rep.Some(reliable)).shaped.<>({ r => import r._; _1.map(_ => SmDeviceRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7.get, _8.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column NAME SqlType(varchar) */
    val name: Rep[String] = column[String]("NAME")
    /** Database column LABEL SqlType(varchar) */
    val label: Rep[String] = column[String]("LABEL")
    /** Database column UID SqlType(varchar) */
    val uid: Rep[String] = column[String]("UID")
    /** Database column SYNC_DATE SqlType(timestamp) */
    val syncDate: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("SYNC_DATE")
    /** Database column DESCRIBE SqlType(varchar), Default(None) */
    val describe: Rep[Option[String]] = column[Option[String]]("DESCRIBE", O.Default(None))
    /** Database column VISIBLE SqlType(bool), Default(true) */
    val visible: Rep[Boolean] = column[Boolean]("VISIBLE", O.Default(true))
    /** Database column RELIABLE SqlType(bool), Default(true) */
    val reliable: Rep[Boolean] = column[Boolean]("RELIABLE", O.Default(true))
  }

  /** Collection-like TableQuery object for table SmDevice */
  lazy val SmDevice = new TableQuery(tag => new SmDevice(tag))

  /** Entity class storing rows of table SmFileCard
    *
    * @param id                Database column ID SqlType(varchar), PrimaryKey
    * @param storeName         Database column STORE_NAME SqlType(varchar)
    * @param fParent           Database column F_PARENT SqlType(varchar)
    * @param fName             Database column F_NAME SqlType(varchar)
    * @param fExtension        Database column F_EXTENSION SqlType(varchar), Default(None)
    * @param fCreationDate     Database column F_CREATION_DATE SqlType(timestamp)
    * @param fLastModifiedDate Database column F_LAST_MODIFIED_DATE SqlType(timestamp)
    * @param fSize             Database column F_SIZE SqlType(int8), Default(None)
    * @param fMimeTypeJava     Database column F_MIME_TYPE_JAVA SqlType(varchar), Default(None)
    * @param sha256            Database column SHA256 SqlType(varchar), Default(None)
    * @param fNameLc           Database column F_NAME_LC SqlType(varchar) */
  case class SmFileCardRow(id: String,
                           storeName: String,
                           fParent: String,
                           fName: String,
                           fExtension: Option[String] = None,
                           fCreationDate: java.time.LocalDateTime,
                           fLastModifiedDate: java.time.LocalDateTime,
                           fSize: Option[Long] = None,
                           fMimeTypeJava: Option[String] = None,
                           sha256: Option[String] = None,
                           fNameLc: String)

  /** GetResult implicit for fetching SmFileCardRow objects using plain SQL queries */
  implicit def GetResultSmFileCardRow(implicit e0: GR[String],
                                      e1: GR[Option[String]],
                                      e2: GR[java.time.LocalDateTime],
                                      e3: GR[Option[Long]]): GR[SmFileCardRow] = GR {
    prs =>
      import prs._
      SmFileCardRow.tupled((<<[String],
        <<[String],
        <<[String],
        <<[String],
        <<?[String],
        <<[java.time.LocalDateTime],
        <<[java.time.LocalDateTime],
        <<?[Long],
        <<?[String],
        <<?[String],
        <<[String]))
  }

  /** Table description of table sm_file_card. Objects of this class serve as prototypes for rows in queries. */
  class SmFileCard(_tableTag: Tag) extends profile.api.Table[SmFileCardRow](_tableTag, "sm_file_card") {
    def * = (id, storeName, fParent, fName, fExtension, fCreationDate, fLastModifiedDate, fSize, fMimeTypeJava, sha256, fNameLc) <> (SmFileCardRow.tupled, SmFileCardRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(storeName), Rep.Some(fParent), Rep.Some(fName), fExtension, Rep.Some(fCreationDate), Rep.Some(fLastModifiedDate), fSize, fMimeTypeJava, sha256, Rep.Some(fNameLc)).shaped.<>({ r => import r._; _1.map(_ => SmFileCardRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8, _9, _10, _11.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Rep[String] = column[String]("ID", O.PrimaryKey)

    val storeName: Rep[String] = column[String]("STORE_NAME")

    val fParent: Rep[String] = column[String]("F_PARENT")

    val fName: Rep[String] = column[String]("F_NAME")

    val fExtension: Rep[Option[String]] = column[Option[String]]("F_EXTENSION", O.Default(None))

    val fCreationDate: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("F_CREATION_DATE")

    val fLastModifiedDate: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("F_LAST_MODIFIED_DATE")

    val fSize: Rep[Option[Long]] = column[Option[Long]]("F_SIZE", O.Default(None))

    val fMimeTypeJava: Rep[Option[String]] = column[Option[String]]("F_MIME_TYPE_JAVA", O.Default(None))

    val sha256: Rep[Option[String]] = column[Option[String]]("SHA256", O.Default(None))

    val fNameLc: Rep[String] = column[String]("F_NAME_LC")

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
    * @param id        Database column ID SqlType(serial), AutoInc, PrimaryKey
    * @param storeName Database column STORE_NAME SqlType(varchar)
    * @param pathFrom  Database column PATH_FROM SqlType(varchar)
    * @param pathTo    Database column PATH_TO SqlType(varchar) */
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
    def ? = (Rep.Some(id), Rep.Some(storeName), Rep.Some(pathFrom), Rep.Some(pathTo)).shaped.<>({ r => import r._; _1.map(_ => SmPathMoveRow.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Rep[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)

    val storeName: Rep[String] = column[String]("STORE_NAME")

    val pathFrom: Rep[String] = column[String]("PATH_FROM")

    val pathTo: Rep[String] = column[String]("PATH_TO")
  }

  /** Collection-like TableQuery object for table SmPathMove */
  lazy val SmPathMove = new TableQuery(tag => new SmPathMove(tag))
}

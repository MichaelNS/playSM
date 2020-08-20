package models.db

// AUTO-GENERATED Slick data model for table SmLog
trait SmLogTable {

  self: Tables =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmLog
    *
    * @param createDate Database column create_date SqlType(timestamp)
    * @param deviceUid  Database column device_uid SqlType(varchar)
    * @param level      Database column level SqlType(varchar)
    * @param step       Database column step SqlType(varchar)
    * @param error      Database column error SqlType(varchar)
    * @param stackTrace Database column stack_trace SqlType(varchar), Default(None) */
  case class SmLogRow(createDate: Option[java.time.LocalDateTime], deviceUid: String, level: String, step: String, error: String, stackTrace: Option[String] = None)

  /** GetResult implicit for fetching SmLogRow objects using plain SQL queries */
  implicit def GetResultSmLogRow(implicit e0: GR[Option[java.time.LocalDateTime]], e1: GR[String], e2: GR[Option[String]]): GR[SmLogRow] = GR {
    prs =>
      import prs._
      SmLogRow.tupled((<<?[java.time.LocalDateTime], <<[String], <<[String], <<[String], <<[String], <<?[String]))
  }

  /** Table description of table sm_log. Objects of this class serve as prototypes for rows in queries. */
  class SmLog(_tableTag: Tag) extends profile.api.Table[SmLogRow](_tableTag, "sm_log") {
    def * = (createDate, deviceUid, level, step, error, stackTrace) <> (SmLogRow.tupled, SmLogRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (createDate, Rep.Some(deviceUid), Rep.Some(level), Rep.Some(step), Rep.Some(error), stackTrace).shaped.<>({ r => import r._; _2.map(_ => SmLogRow.tupled((_1, _2.get, _3.get, _4.get, _5.get, _6))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

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
    lazy val smDeviceFk = foreignKey("fk_sm_log_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }

  /** Collection-like TableQuery object for table SmLog */
  lazy val SmLog = new TableQuery(tag => new SmLog(tag))
}

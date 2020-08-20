package models.db

// AUTO-GENERATED Slick data model for table SmJobPathMove
trait SmJobPathMoveTable {

  self: Tables =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmJobPathMove
    *
    * @param id        Database column id SqlType(serial), AutoInc
    * @param deviceUid Database column device_uid SqlType(varchar)
    * @param pathFrom  Database column path_from SqlType(varchar)
    * @param pathTo    Database column path_to SqlType(varchar)
    * @param done      Database column done SqlType(timestamp), Default(None) */
  case class SmJobPathMoveRow(id: Int, deviceUid: String, pathFrom: String, pathTo: String, done: Option[java.time.LocalDateTime] = None)

  /** GetResult implicit for fetching SmJobPathMoveRow objects using plain SQL queries */
  implicit def GetResultSmJobPathMoveRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[java.time.LocalDateTime]]): GR[SmJobPathMoveRow] = GR {
    prs =>
      import prs._
      SmJobPathMoveRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<?[java.time.LocalDateTime]))
  }

  /** Table description of table sm_job_path_move. Objects of this class serve as prototypes for rows in queries. */
  class SmJobPathMove(_tableTag: Tag) extends profile.api.Table[SmJobPathMoveRow](_tableTag, "sm_job_path_move") {
    def * = (id, deviceUid, pathFrom, pathTo, done) <> (SmJobPathMoveRow.tupled, SmJobPathMoveRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(deviceUid), Rep.Some(pathFrom), Rep.Some(pathTo), done).shaped.<>({ r => import r._; _1.map(_ => SmJobPathMoveRow.tupled((_1.get, _2.get, _3.get, _4.get, _5))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

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
    lazy val smDeviceFk = foreignKey("fk_sm_job_path_move_sm_device", deviceUid, SmDevice)(r => r.uid, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (id) (database name unq_sm_job_path_move) */
    val index1 = index("unq_sm_job_path_move", id, unique = true)
  }

  /** Collection-like TableQuery object for table SmJobPathMove */
  lazy val SmJobPathMove = new TableQuery(tag => new SmJobPathMove(tag))
}

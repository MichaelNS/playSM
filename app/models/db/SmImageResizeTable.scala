package models.db

// AUTO-GENERATED Slick data model for table SmImageResize
trait SmImageResizeTable {

  self: Tables =>

  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmImageResize
    *
    * @param fileId Database column file_id SqlType(varchar), PrimaryKey
    * @param sha256 Database column sha256 SqlType(varchar)
    * @param fName  Database column f_name SqlType(varchar) */
  case class SmImageResizeRow(fileId: String, sha256: String, fName: String)

  /** GetResult implicit for fetching SmImageResizeRow objects using plain SQL queries */
  implicit def GetResultSmImageResizeRow(implicit e0: GR[String]): GR[SmImageResizeRow] = GR {
    prs =>
      import prs._
      SmImageResizeRow.tupled((<<[String], <<[String], <<[String]))
  }

  /** Table description of table sm_image_resize. Objects of this class serve as prototypes for rows in queries. */
  class SmImageResize(_tableTag: Tag) extends profile.api.Table[SmImageResizeRow](_tableTag, "sm_image_resize") {
    def * = (fileId, sha256, fName) <> (SmImageResizeRow.tupled, SmImageResizeRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(fileId), Rep.Some(sha256), Rep.Some(fName)).shaped.<>({ r => import r._; _1.map(_ => SmImageResizeRow.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column file_id SqlType(varchar), PrimaryKey */
    val fileId: Rep[String] = column[String]("file_id", O.PrimaryKey)
    /** Database column sha256 SqlType(varchar) */
    val sha256: Rep[String] = column[String]("sha256")
    /** Database column f_name SqlType(varchar) */
    val fName: Rep[String] = column[String]("f_name")

    /** Uniqueness Index over (sha256,fName) (database name sm_image_resize_uniq) */
    val index1 = index("sm_image_resize_uniq", (sha256, fName), unique = true)
  }

  /** Collection-like TableQuery object for table SmImageResize */
  lazy val SmImageResize = new TableQuery(tag => new SmImageResize(tag))
}

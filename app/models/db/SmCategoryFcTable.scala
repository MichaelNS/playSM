package models.db

// AUTO-GENERATED Slick data model for table SmCategoryFc
trait SmCategoryFcTable {

  self: Tables =>

  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmCategoryFc
    *
    * @param id     Database column id SqlType(int4)
    * @param sha256 Database column sha256 SqlType(varchar)
    * @param fName  Database column f_name SqlType(varchar) */
  case class SmCategoryFcRow(id: Int, sha256: String, fName: String)

  /** GetResult implicit for fetching SmCategoryFcRow objects using plain SQL queries */
  implicit def GetResultSmCategoryFcRow(implicit e0: GR[Int], e1: GR[String]): GR[SmCategoryFcRow] = GR {
    prs =>
      import prs._
      SmCategoryFcRow.tupled((<<[Int], <<[String], <<[String]))
  }

  /** Table description of table sm_category_fc. Objects of this class serve as prototypes for rows in queries. */
  class SmCategoryFc(_tableTag: Tag) extends profile.api.Table[SmCategoryFcRow](_tableTag, "sm_category_fc") {
    def * = (id, sha256, fName) <> (SmCategoryFcRow.tupled, SmCategoryFcRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(sha256), Rep.Some(fName)).shaped.<>({ r => import r._; _1.map(_ => SmCategoryFcRow.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4) */
    val id: Rep[Int] = column[Int]("id")
    /** Database column sha256 SqlType(varchar) */
    val sha256: Rep[String] = column[String]("sha256")
    /** Database column f_name SqlType(varchar) */
    val fName: Rep[String] = column[String]("f_name")

    /** Primary key of SmCategoryFc (database name sm_category_fc_pkey) */
    val pk = primaryKey("sm_category_fc_pkey", (sha256, fName))

    /** Foreign key referencing SmCategoryRule (database name fk_sm_category_fc_sm_category_rule) */
    lazy val smCategoryRuleFk = foreignKey("fk_sm_category_fc_sm_category_rule", id, SmCategoryRule)(r => r.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table SmCategoryFc */
  lazy val SmCategoryFc = new TableQuery(tag => new SmCategoryFc(tag))
}

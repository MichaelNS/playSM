package models.db

// AUTO-GENERATED Slick data model for table SmCategoryRule
trait SmCategoryRuleTable {

  self: Tables =>

  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** Entity class storing rows of table SmCategoryRule
    *
    * @param id           Database column id SqlType(serial), AutoInc
    * @param categoryType Database column category_type SqlType(varchar)
    * @param category     Database column category SqlType(varchar)
    * @param subCategory  Database column sub_category SqlType(varchar)
    * @param fPath        Database column f_path SqlType(_text)
    * @param isBegins     Database column is_begins SqlType(bool)
    * @param description  Database column description SqlType(varchar), Default(None) */
  case class SmCategoryRuleRow(id: Int, categoryType: String, category: String, subCategory: String, fPath: List[String], isBegins: Boolean, description: Option[String] = None)

  /** GetResult implicit for fetching SmCategoryRuleRow objects using plain SQL queries */
  implicit def GetResultSmCategoryRuleRow(implicit e0: GR[Int], e1: GR[String], e2: GR[List[String]], e3: GR[Boolean], e4: GR[Option[String]]): GR[SmCategoryRuleRow] = GR {
    prs =>
      import prs._
      SmCategoryRuleRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[List[String]], <<[Boolean], <<?[String]))
  }

  /** Table description of table sm_category_rule. Objects of this class serve as prototypes for rows in queries. */
  class SmCategoryRule(_tableTag: Tag) extends profile.api.Table[SmCategoryRuleRow](_tableTag, "sm_category_rule") {
    def * = (id, categoryType, category, subCategory, fPath, isBegins, description) <> (SmCategoryRuleRow.tupled, SmCategoryRuleRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(categoryType), Rep.Some(category), Rep.Some(subCategory), Rep.Some(fPath), Rep.Some(isBegins), description).shaped.<>({ r => import r._; _1.map(_ => SmCategoryRuleRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc */
    val id: Rep[Int] = column[Int]("id", O.AutoInc)
    /** Database column category_type SqlType(varchar) */
    val categoryType: Rep[String] = column[String]("category_type")
    /** Database column category SqlType(varchar) */
    val category: Rep[String] = column[String]("category")
    /** Database column sub_category SqlType(varchar) */
    val subCategory: Rep[String] = column[String]("sub_category")
    /** Database column f_path SqlType(_text) */
    val fPath: Rep[List[String]] = column[List[String]]("f_path")
    /** Database column is_begins SqlType(bool) */
    val isBegins: Rep[Boolean] = column[Boolean]("is_begins")
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))

    /** Uniqueness Index over (categoryType,category,subCategory) (database name sm_category_rule_pkey) */
    val index1 = index("sm_category_rule_pkey", (categoryType, category, subCategory), unique = true)
    /** Uniqueness Index over (id) (database name unq_sm_category_rule_id) */
    val index2 = index("unq_sm_category_rule_id", id, unique = true)
  }

  /** Collection-like TableQuery object for table SmCategoryRule */
  lazy val SmCategoryRule = new TableQuery(tag => new SmCategoryRule(tag))
}

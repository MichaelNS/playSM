package models.db

// AUTO-GENERATED Slick data model [2020-08-20T10:45:40.351253100+03:00[Europe/Moscow]]
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = utils.db.SmPostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.)
  * Each generated XXXXTable trait is mixed in this trait hence allowing access to all the TableQuery lazy vals.
  */
trait Tables extends SmDeviceScanTable with SmLogTable with SmFileCardTable with SmCategoryFcTable with SmImageResizeTable with SmCategoryRuleTable with SmExifTable with SmDeviceTable with SmJobPathMoveTable {
  val profile: utils.db.SmPostgresDriver
  import profile.api._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(SmCategoryFc.schema, SmCategoryRule.schema, SmDevice.schema, SmDeviceScan.schema, SmExif.schema, SmFileCard.schema, SmImageResize.schema, SmJobPathMove.schema, SmLog.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl: profile.DDL = schema

}

package utils.db

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

trait SmPostgresDriver extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  //  with PgEnumSupport
  //  with PgRangeSupport
  //  with PgHStoreSupport
  //  with PgNetSupport
  //  with PgLTreeSupport
  //  with PgSearchSupport
{
  def pgjson: String = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api: SmPgAPI.type = SmPgAPI

  object SmPgAPI extends API
    with DateTimeImplicits
    //    with ArrayImplicits
    //    with NetImplicits
    //    with LTreeImplicits
    //    with RangeImplicits
    //    with HStoreImplicits
    //    with SearchImplicits
    //    with SearchAssistants
  {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

}

object SmPostgresDriver extends SmPostgresDriver

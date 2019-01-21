package utils.db

import com.github.tminglei.slickpg._

trait SmPostgresDriver extends ExPostgresProfile
  with PgArraySupport
  with PgEnumSupport
  with PgRangeSupport
  with PgDate2Support
  with PgHStoreSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {

  override val api: SmPgAPI.type = SmPgAPI

  object SmPgAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {

    implicit val strListTypeMapper: DriverJdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

}

object SmPostgresDriver extends SmPostgresDriver

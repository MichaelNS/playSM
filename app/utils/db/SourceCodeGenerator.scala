package utils.db

import java.io.File

import com.typesafe.config.ConfigFactory
import slick.sql.SqlProfile.ColumnOption

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Based on https://github.com/tminglei/slick-pg/blob/master/examples/codegen-customization/codegen/src/main/scala/demo/CustomizedCodeGenerator.scala
  *
  * runMain utils.db.SourceCodeGenerator
  */
object SourceCodeGenerator extends App {

  val config = ConfigFactory.parseFile(new File("conf/application.conf"))

  val databaseURL = config.getString("slick.dbs.default.db.url")
  val databaseUser = config.getString("slick.dbs.default.db.user")
  val databasePassword = config.getString("slick.dbs.default.db.password")
  val jdbcDriver = config.getString("slick.dbs.default.db.profile")

  val slickDriver = {
    val value = config.getString("slick.dbs.default.profile")

    //remove last $ character
    val pos = value.length - 1
    if (value.charAt(pos) == '$') {
      value.substring(0, pos)
    } else {
      value
    }
  }
  val generatedFileClass = "Tables"
  val generatedFilePackage = "models.db"
  val generatedFileName = "Tables.scala"
  val generatedFileOutputFolder = "app"

  val db = SmPostgresDriver.api.Database.forURL(
    url = databaseURL,
    driver = jdbcDriver,
    user = databaseUser,
    password = databasePassword
  )

  //the table spatial_ref_sys is an internal table of the postgis extension
  val filteredTables = SmPostgresDriver.defaultTables.map(_.filter(t => t.name.name.toLowerCase().startsWith("sm_") && !t.name.name.toLowerCase.endsWith("_back")))
  val modelAction = SmPostgresDriver.createModel(Some(filteredTables))

  val codegen = db.run(modelAction).map { model =>

    new slick.codegen.SourceCodeGenerator(model) {
      override def Table = new Table(_) {
        table =>

        override def Column = new Column(_) {
          column =>
          // customize db type -> scala type mapping, pls adjust it according to your environment
          override def rawType: String = model.tpe match {
            case "java.sql.Date" => "java.time.LocalDate"
            case "java.sql.Time" => "java.time.LocalTime"
            case "java.sql.Timestamp" => "java.time.LocalDateTime"

            // currently, all types that's not built-in support were mapped to `String`
            case "String" => model.options.find(_.isInstanceOf[ColumnOption.SqlType])
              .map(_.asInstanceOf[ColumnOption.SqlType].typeName).map({

              //array of text
              case "_text" => "List[String]"

              case "text" => "String"
              case "varchar" => "String"

              case unknown => throw new IllegalArgumentException(s"Undefined type [$unknown]")

            }).getOrElse("String")
            case _ => super.rawType
          }
        }
      }

      override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]): String = {
        s"""
package $pkg

// AUTO-GENERATED Slick data model [${java.time.ZonedDateTime.now()}]

/** Stand-alone Slick data model for immediate use */
object $container extends {
  val profile = $profile
} with $container

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait $container${parentType.map(t => s" extends $t").getOrElse("")} {
  val profile: $profile
  import profile.api._
  ${indent(code)}
}
              """.trim()
      }
    }
  }

  Await.ready(codegen.map(_.writeToFile(slickDriver, generatedFileOutputFolder, generatedFilePackage, generatedFileClass, generatedFileName)), Duration.Inf)
}

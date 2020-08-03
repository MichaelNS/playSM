import org.joda.time.DateTime
import play.api.Logger
import slick.jdbc.GetResult

package object controllers {

  implicit val getDateTimeResult: AnyRef with GetResult[DateTime] = GetResult(r => new DateTime(r.nextTimestamp()))

  val logger: Logger = play.api.Logger(getClass)

  def debugParam(implicit line: sourcecode.Line, enclosing: sourcecode.Enclosing, args: sourcecode.Args): Unit = {
    logger.debug(s"debugParam ${enclosing.value} : ${line.value}  - "
      + args.value.map(_.map(a => a.source + s"=[${a.value}]").mkString("(", ", ", ")")).mkString("")
    )
  }

  def debug[V](value: sourcecode.Text[V])(implicit fullName: sourcecode.FullName): Unit = {
    logger.debug(s"${fullName.value} = ${value.source} : [${value.value}]")
  }
}

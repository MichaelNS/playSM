import play.api.Logger

package object controllers {

  def debugParam(implicit line: sourcecode.Line, enclosing: sourcecode.Enclosing, args: sourcecode.Args): Unit = {
    Logger.debug(s"debugParam ${enclosing.value} : ${line.value}  - "
      + args.value.map(_.map(a => a.source + s"=[${a.value}]").mkString("(", ", ", ")")).mkString("")
    )
  }

  def debug[V](value: sourcecode.Text[V])(implicit fullName: sourcecode.FullName): Unit = {
    Logger.debug(s"${fullName.value} = ${value.source} : [${value.value}]")
  }
}

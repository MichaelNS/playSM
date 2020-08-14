package ru.ns.model

import scala.util.Properties

/**
  * Current operation system config
  * [[OsConf.fsSeparator]] FileSystems.getDefault.getSeparator
  * <p>
  * [[OsConf.isLinux]] - Linux OS environment
  * <p>
  * [[OsConf.isWindows]] - Windows OS environment
  * <p>
  * [[OsConf.isMac]] - Apple Mac OSX environment
  *
  */
trait OsConfMethods {
  def isWindows: Boolean

  def getOsSeparator: String

  def getMacWinDeviceRegexp: String
}

object OsConf extends OsConfMethods {

  val fsSeparator: String = "/"
  val winDeviceRegExp: String = "\\(([^)]+)\\)"
  val macOsDeviceRegExp: String = "(.*)\\s+(\\([^\\)]+\\))"

  def isLinux: Boolean = Properties.isLinux

  def isWindows: Boolean = Properties.isWin

  def isMac: Boolean = Properties.isMac

  def getOsSeparator: String = if (isWindows) "\\" else "/"

  def getMacWinDeviceRegexp: String = if (isWindows) winDeviceRegExp else macOsDeviceRegExp
}

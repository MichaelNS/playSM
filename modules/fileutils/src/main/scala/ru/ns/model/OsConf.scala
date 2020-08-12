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




object OsConf {

  val fsSeparator: String = "/"

  def isLinux: Boolean = Properties.isLinux

  def isWindows: Boolean = Properties.isWin

  def isMac: Boolean = Properties.isMac

  def getOsSeparator: String = if (Properties.isWin) "\\" else "/"

  def getMacWinDeviceRegexp: String = if (Properties.isWin) "\\(([^)]+)\\)" else "(.*)\\s+(\\([^\\)]+\\))"
}

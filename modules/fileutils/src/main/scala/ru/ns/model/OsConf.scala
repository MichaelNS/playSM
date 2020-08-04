package ru.ns.model

/**
  * Current operation system config
  * [[OsConf.fsSeparator]] FileSystems.getDefault.getSeparator
  * <p>
  * [[OsConf.isUnix]] - Nix OS environment
  * <p>
  * [[OsConf.isWindows]] - Windows OS environment
  */
object OsConf {

  val fsSeparator: String = "/"

  val OS: String = System.getProperty("os.name").toLowerCase

  def isUnix: Boolean = OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0

  def isWindows: Boolean = OS.indexOf("win") >= 0

  def isMac: Boolean = OS.indexOf("mac") >= 0

  def getOsSeparator: String = if (isWindows) "\\" else "/"

  def getMacWinDeviceRegexp: String = if (isWindows) "\\(([^)]+)\\)" else "(.*)\\s+(\\([^\\)]+\\))"
}

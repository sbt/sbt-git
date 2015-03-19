package com.typesafe.sbt.git

import sbt._
import Keys._

/** A mechanism of running git that simply shells out to the console. */
object ConsoleGitRunner extends GitRunner {
  // TODO - Something less lame here.
  def isWindowsShell = {
		val ostype = System.getenv("OSTYPE")
		val isCygwin = ostype != null && ostype.toLowerCase.contains("cygwin")
		val isWindows = System.getProperty("os.name", "").toLowerCase.contains("windows")
		isWindows && !isCygwin
	}
  private lazy val cmd = if(isWindowsShell) Seq("cmd", "/c", "git") else Seq("git")

  // in order to enable colors we trick git into thinking we're a pager, because it already knows we're not a tty
  val colorSupport = ("GIT_PAGER_IN_USE", "1")
  
  override def apply(args: String*)(cwd: File, log: Logger = ConsoleLogger()): String = {
    val gitLogger = new GitLogger(log)
    IO.createDirectory(cwd)
    val full = cmd ++ args
    log.debug(cwd + "$ " + full.mkString(" "))
    val code = Process(full, cwd, colorSupport) ! gitLogger
    val result = gitLogger.flush(code)
    if(code != 0)
      error("Nonzero exit code (" + code + ") running git.")
    else
      result
  }

  override def toString = "git"
  // reduce log level for git process
  private class GitLogger(log: Logger) extends ProcessLogger {
    import scala.collection.mutable.ListBuffer
    import Level.{ Info, Warn, Error, Value => LogLevel }

    private val msgs: ListBuffer[(LogLevel, String)] = new ListBuffer()

    def info(s: => String): Unit =
      synchronized { msgs += ((Info, s)) }

    def error(s: => String): Unit =
      synchronized { msgs += ((Error, s)) }

    def buffer[T](f: => T): T = f

    private def print(desiredLevel: LogLevel)(t: (LogLevel, String)): String = t match {
      case (Info, msg) =>
        log.info(msg)
        msg
      case (Error, msg) =>
        log.log(desiredLevel, msg)
        msg
    }

    def flush(exitCode: Int): String = {
      val level = if (exitCode == 0) Info else Error // reduce log level Error -> Info if exitCode is zero
      var result = msgs map print(level)
      msgs.clear()
      result.mkString("\n")
    }
  }

}

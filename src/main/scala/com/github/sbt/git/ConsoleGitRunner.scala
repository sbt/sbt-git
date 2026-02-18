package com.github.sbt.git

import sbt.*
import sbt.internal.util.Terminal

import scala.util.Try
import sys.process.{Process, ProcessLogger}

/** A mechanism of running git that simply shells out to the console. */
object ConsoleGitRunner extends GitRunner {
  // TODO - Something less lame here.
  def isWindowsShell: Boolean = {
    val ostype = System.getenv("OSTYPE")
    val isCygwin = ostype != null && ostype.toLowerCase.contains("cygwin")
    val isWindows = System.getProperty("os.name", "").toLowerCase.contains("windows")
    isWindows && !isCygwin
  }
  private lazy val cmd = if (isWindowsShell) Seq("cmd", "/c", "git") else Seq("git")

  // in order to enable colors we trick git into thinking we're a pager, because it already knows we're not a tty
  val colorSupport: Seq[(String, String)] =
    Try {
      if (Terminal.console.isAnsiSupported)
        Seq("GIT_PAGER_IN_USE" -> "1")
      else
        Seq.empty
    }.getOrElse(Seq.empty)

  override def apply(args: String*)(cwd: File, log: Logger = ConsoleLogger()): String = {
    val gitLogger = new GitLogger(log)
    IO.createDirectory(cwd)
    val full = cmd ++ args
    log.debug(cwd.toString + "$ " + full.mkString(" "))
    val code = Process(full, cwd, colorSupport*) ! gitLogger
    val result = gitLogger.flush(code)
    if (code != 0)
      throw new MessageOnlyException("Nonzero exit code (" + code + ") running git.")
    else
      result
  }

  override def toString = "git"
  // reduce log level for git process
  private class GitLogger(log: Logger) extends ProcessLogger {
    import scala.collection.mutable.ListBuffer
    import Level.{Debug, Info, Error, Value as LogLevel}

    private val msgs: ListBuffer[(LogLevel, String)] = new ListBuffer()

    def info(s: => String): Unit =
      synchronized { msgs += ((Debug, s)) }

    def error(s: => String): Unit =
      synchronized { msgs += ((Error, s)) }

    def err(s: => String): Unit = error(s)

    def out(s: => String): Unit = info(s)

    def buffer[T](f: => T): T = f

    private def print(desiredLevel: LogLevel)(t: (LogLevel, String)): String = t match {
      case (Debug, msg) =>
        log.debug(msg)
        msg
      case (Info, msg) =>
        log.info(msg)
        msg
      case (Error, msg) =>
        log.log(desiredLevel, msg)
        msg
    }

    def flush(exitCode: Int): String = {
      val level = if (exitCode == 0) Debug else Error // reduce log level Error -> Debug if exitCode is zero
      var result = msgs map print(level)
      msgs.clear()
      result.mkString("\n")
    }
  }

}

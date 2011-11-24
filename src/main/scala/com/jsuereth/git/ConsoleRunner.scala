package com.jsuereth.git

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
  override def apply(args: String*)(cwd: File, log: Logger): Unit = {
      IO.createDirectory(cwd)
      val full = cmd ++ args
      log.info(cwd + "$ " + full.mkString(" "))
      val code = Process(full, cwd) ! log
      if(code != 0) error("Nonzero exit code for git " + args.take(1).mkString + ": " + code)
  }
  override def toString = "git"
}

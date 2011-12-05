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
  override def apply(args: String*)(cwd: File, log: Logger): String = {
      IO.createDirectory(cwd)
      val full = cmd ++ args
      log.info(cwd + "$ " + full.mkString(" "))
      Process(full, cwd) !! log
  }
  override def toString = "git"
}

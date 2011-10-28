package com.jsuereth.git

import sbt._
import Keys._

/** A mechanism of running git that simply shells out to the console. */
object ConsoleGitRunner extends GitRunner {
  // TODO - Something less lame here.
  private lazy val cmd = if(System.getProperty("os.name").toLowerCase().contains("windows")) "git.exe" else "git"
  override def apply(args: String*)(cwd: File, log: Logger): Unit = {
      IO.createDirectory(cwd)
      val full = "git" +: args
      log.info(cwd + "$ " + full.mkString(" "))
      val code = Process(full, cwd) ! log
      if(code != 0) error("Nonzero exit code for git " + args.take(1).mkString + ": " + code)
  }
  override def toString = "git"
}

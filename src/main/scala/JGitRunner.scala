package com.typesafe.git 

import sbt._
import Keys._
import org.eclipse.jgit.pgm.{Main=>JGit}

/** A mechanism to run GIT using the pure java JGit implementation. */
object JGitRunner extends GitRunner {
  override def apply(args: String*)(cwd: File, log: Logger): Unit =
    // TODO -  Can we just assume the .git repo? I hope so....
    JGit.main((Seq("--git-dir", cwd.getAbsolutePath + "/.git") ++ args).toArray)  
  override def toString = "jgit"  
}


package com.typesafe.git 

import sbt._
import Keys._
import org.eclipse.jgit.pgm.{Main=>JGit}

/** A mechanism to run GIT using the pure java JGit implementation. */
object JGitRunner extends GitRunner {
  override def apply(args: String*)(cwd: File, log: Logger = ConsoleLogger()): Unit = {
    // TODO -  Can we just assume the .git repo? I hope so....
    //JGit.main((Seq("--git-dir", cwd.getAbsolutePath + "/.git") ++ args).toArray)  
    // Make a good ole fashioned classpath.
    getClass.getClassLoader match {
       case cl: java.net.URLClassLoader =>
         val cp = cl.getURLs map (_.getFile) mkString ":"
         Fork.java(None, Seq("-classpath", cp, "org.eclipse.jgit.pgm.Main") ++ args, Some(cwd), log)         
       case _ => log.error("Could not find classpath for JGit!")
    }

  }
  override def toString = "jgit"  
}


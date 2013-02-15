package com.typesafe.sbt.git

import sbt._
import Keys._

/** A mechanism to run GIT using the pure java JGit implementation. */
object JGitRunner extends GitRunner {

  override def apply(args: String*)(cwd: File, log: Logger = ConsoleLogger()): String = {
    args.headOption match {
      case Some("push") => push(args drop 1)(cwd, log)
      case Some("pull") => pull(args drop 1)(cwd, log)
      case Some("clone") => clone(args drop 1)(cwd, log)
      case _ =>  forkJGitMain(args:_*)(cwd, log)
    }
  }


  /**
       git clone [--template=<template_directory>]
                 [-l] [-s] [--no-hardlinks] [-q] [-n] [--bare] [--mirror]
                 [-o <name>] [-b <name>] [-u <upload-pack>] [--reference <repository>]
                 [--separate-git-dir <git dir>]
                 [--depth <depth>] [--[no-]single-branch]
                 [--recursive|--recurse-submodules] [--] <repository>
                 [<directory>]
   */
  private def clone(args: Seq[String])(cwd: File, log: Logger = ConsoleLogger()): String = {
    // TODO - Parse args...
    // TODO - this should not just work for ghpages plugin
    val git = JGit(cwd)
    args match {
      case Seq("-b", branch, remote, ".") =>
        // TODO - Logging?
        JGit.clone(remote, cwd).checkoutBranch(branch);
      case Seq(remote, ".") =>
        JGit.clone(remote, cwd)
      case _ => sys.error("Unable to run clone command: clone " + args.mkString(" "))
    }
    ""
  }

  private def pull(args: Seq[String])(cwd: File, log: Logger = ConsoleLogger()): String = {
    val git = JGit(cwd)
    // TODO - Parse options...
    // TODO - Set logging/progress monitor.
    git.porcelain.pull().call();
    ""
  }

   private def push(args: Seq[String])(cwd: File, log: Logger = ConsoleLogger()): String = {
    val git = JGit(cwd)
    // TODO - Parse options...
    // TODO - Set logging/progress monitor.
    git.porcelain.push().call();
    ""
  }

  private def forkJGitMain(args: String*)(cwd: File, log: Logger = ConsoleLogger()): String =
    getClass.getClassLoader match {
       case cl: java.net.URLClassLoader =>
         val cp = cl.getURLs map (_.getFile) mkString ":"
         // TODO - this is horrible, horrible code.  Can we do this a safe string way?
         // See if we can add the !! method to Fork.java
         object output extends java.io.OutputStream {
           val mBuf = new StringBuilder
           override def write(byte: Int) = mBuf append byte.toChar
           def value = mBuf.toString
         }
         Fork.java(None, Seq("-classpath", cp, "org.eclipse.jgit.pgm.Main") ++ args, Some(cwd), CustomOutput(output))
         val result = output.value
         log.info(result)
         result
       case _ => sys.error("Could not find classpath for JGit!")
    }

  override def toString = "jgit"

}


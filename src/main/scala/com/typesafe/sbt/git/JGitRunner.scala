package com.typesafe.sbt.git

import sbt._
import Keys._
import Compat._

/** A mechanism to run GIT using the pure java JGit implementation. */
object JGitRunner extends GitRunner {

  override def apply(args: String*)(cwd: File, log: Logger = ConsoleLogger()): String = {
    args.headOption match {
      // For now, let's just use the JGIT comand line and see what happens.
      // Eventually we can aim to speed up commands...
      //case Some("push") => push(args drop 1)(cwd, log)
      //case Some("pull") => pull(args drop 1)(cwd, log)
      //case Some("clone") => clone(args drop 1)(cwd, log)
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
         val baos = new java.io.ByteArrayOutputStream
         // NOTE: this will always return 0 until sbt 0.13.1 due to the use of CustomOutput
         val code = Fork.java.fork(forkOpt(baos, cwd),
           Vector("-classpath", cp, "org.eclipse.jgit.pgm.Main") ++ args).exitValue()
         val result = baos.toString
         log.info(result)
         if(code == 0) result else throw new MessageOnlyException("Nonzero exit code (" + code + ") running JGit.")
       case _ => sys.error("Could not find classpath for JGit!")
    }

  override def toString = "jgit"

}

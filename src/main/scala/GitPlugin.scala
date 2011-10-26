package com.typesafe.git 

import sbt._
import Keys._
import org.eclipse.jgit.pgm.{Main=>JGit}

/** Keys relative to using git from SBT */
object GitKeys {
  val gitRemoteRepo = SettingKey[String]("git-remote-repo", "The remote git repository assoicated with this project") 
  val gitRunner = TaskKey[GitRunner]("git-runner", "The mechanism used to run git in the current build.")
}

/** This plugin has all the basic 'git' functionality for other plugins. */
object GitPlugin extends Plugin {
  import GitKeys._
  // TODO - Should we embedd everywhere like this?
  override val settings = Seq(
    gitRunner in ThisBuild := ConsoleGitRunner
    // TODO - add an inputtask that can run git inside SBT.
  )

  def useJGit = gitRunner in ThisBuild := JGitRunner
}


/** An interface to run git commands. */
trait GitRunner {
  /** Akin to running 'git {args}' with a given working directory `cwd` and logger. */
  def apply(args: String*)(cwd: File, log: Logger): Unit
  /** Commits all local changes and pushes the new commit to a remote repository. */
  def commitAndPush(msg: String, tag: Option[String] = None)(repo: File, log: Logger) {
    apply("add", ".")(repo, log)
    apply("commit", "-m", msg, "--allow-empty")(repo, log)
    for(tagString <- tag) apply("tag", tagString)(repo, log)
    push(repo, log)
  }
  /** Pushes local commits to the remote branch. */
  def push(cwd: File, log: Logger) = apply("push")(cwd, log)
  /** Pulls remote changes into the local branch. */
  def pull(cwd: File, log: Logger) = apply("pull")(cwd, log)
  /** Updates the cwd from a remote branch. If the local git repo doesn't exist, will clone it into existence. */
  def updated(remote: String, branch: Option[String], cwd: File, log: Logger): Unit =
      if(cwd.exists) pull(cwd, log)
      else branch match {
        case None => apply("clone", remote, ".")(cwd, log)
        case Some(b) => apply("clone", "-b", b, remote, ".")(cwd, log)
      }
}

/** A mechanism to run GIT using the pure java JGit implementation. */
object JGitRunner extends GitRunner {
  override def apply(args: String*)(cwd: File, log: Logger): Unit =
    // TODO -  Can we just assume the .git repo? I hope so....
    JGit.main((Seq("--git-dir", cwd.getAbsolutePath + "/.git") ++ args).toArray)  
  override def toString = "jgit"  
}

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



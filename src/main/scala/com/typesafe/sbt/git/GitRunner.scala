package com.typesafe.sbt.git

import sbt._

/** An interface to run git commands. */
trait GitRunner {
  /** Akin to running 'git {args}' with a given working directory `cwd` and logger.
   * This will always return the resulting output string of the process.
   */
  def apply(args: String*)(cwd: File, log: Logger): String
  /** Commits all local changes and pushes the new commit to a remote repository. */
  def commitAndPush(msg: String, tag: Option[String] = None)(repo: File, log: Logger) {
    apply("add", ".")(repo, log)
    apply("commit", "-m", msg, "--allow-empty")(repo, log)
    for(tagString <- tag) apply("tag", tagString)(repo, log)
    push(repo, log)
  }
  def currentBranchOrNone(cwd: File, log: Logger): Option[String] = {
    apply("branch")(cwd, log).split(System.lineSeparator).find{_.head == '*'}.map{_.drop(2)}
  }
  /** Grabs the head commit from git. */
  def headCommit(cwd: File, log: Logger): String =
    apply("rev-parse", "HEAD")(cwd, log)
  /** Returns the most recently created tag name, if it exists, wrapped in an option. */
  def currentTags(cwd: File, log: Logger): Seq[String] = {
    val sha = headCommit(cwd, log)
    // TODO - This seems prone to failure and risky.  JGit is a better option.
    // Also we should parse this a bit better.
    try Seq(apply("describe", "--tags", "--exact-match", sha)(cwd, log))
    catch {
      case e: Exception => Nil
    }
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

  def prompt(state: State)(cwd: File, log: Logger): String =
    currentBranchOrNone(cwd,log).getOrElse("") + "> "
}

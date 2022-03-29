package com.github.sbt.git

import sbt._

/** An interface to run git commands. */
trait GitRunner {
  /** Akin to running 'git {args}' with a given working directory `cwd` and logger.
   * This will always return the resulting output string of the process.
   */
  def apply(args: String*)(cwd: File, log: Logger): String
  /** Commits all local changes and pushes the new commit to a remote repository. */
  def commitAndPush(msg: String, tag: Option[String] = None)(repo: File, log: Logger): Unit = {
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

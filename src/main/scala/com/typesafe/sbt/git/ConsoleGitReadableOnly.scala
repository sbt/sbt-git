package com.typesafe.sbt.git

import scala.util.Try

import sbt.{File, Logger}

class ConsoleGitReadableOnly(git: GitRunner, cwd: File, log: Logger) extends GitReadonlyInterface {
  def branch: String = git("rev-parse", "--abbrev-ref", "HEAD")(cwd, log)

  def headCommitSha: Option[String] = Try(git("rev-parse", "HEAD")(cwd, log)).toOption

  def headCommitDate: Option[String] = Try(git("log", """--pretty="%cI"""", "-n", "1")(cwd, log)).toOption

  def currentTags: Seq[String] = git("tag", "--points-at", "HEAD")(cwd, log).split("\\s+")

  def describedVersion: Option[String] = git("describe", "--tags")(cwd, log).split("\\s+").headOption

  def hasUncommittedChanges: Boolean = git("status")(cwd, log).contains("nothing to commit, working tree clean")

  def branches: Seq[String] = git("branch", "--list")(cwd, log).split("\\s+")

  def remoteBranches: Seq[String] = git("branch", "-l", "--remotes")(cwd, log).split("\\s+")

  def remoteOrigin: String = git("ls-remote", "--get-url", "origin")(cwd, log)

  def headCommitMessage: Option[String] = Try(git("log", "--pretty=%s\n\n%b", "-n", "1")(cwd, log)).toOption
}

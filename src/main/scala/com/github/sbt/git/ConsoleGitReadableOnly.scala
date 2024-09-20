package com.github.sbt.git

import scala.util.Try

import sbt.{File, Logger}

class ConsoleGitReadableOnly(git: GitRunner, cwd: File, log: Logger) extends GitReadonlyInterface {
  def branch: String = git("rev-parse", "--abbrev-ref", "HEAD")(cwd, log)

  def headCommitSha: Option[String] = Try(git("rev-parse", "HEAD")(cwd, log)).toOption

  def headCommitDate: Option[String] = Try(git("log", """--pretty="%cI"""", "-n", "1")(cwd, log)).toOption

  def currentTags: Seq[String] = Try(git("tag", "--points-at", "HEAD")(cwd, log).split("\\s+").toSeq).getOrElse(Seq())

  def describedVersion: Option[String] = Try(git("describe", "--tags")(cwd, log).split("\\s+").headOption).toOption.flatten

  override def describedVersion(patterns: Seq[String]): Option[String] =
    patterns.headOption.fold(describedVersion)(pat =>
      Try(git("describe", "--tags", "--match", pat)(cwd, log).split("\\s+").headOption).toOption.flatten
    )

  def hasUncommittedChanges: Boolean = Try(!git("status", "-s")(cwd, log).trim.isEmpty).getOrElse(true)

  def branches: Seq[String] = Try(git("branch", "--list")(cwd, log).split("\\s+").toSeq).getOrElse(Seq())

  def remoteBranches: Seq[String] = Try(git("branch", "-l", "--remotes")(cwd, log).split("\\s+").toSeq).getOrElse(Seq())

  def remoteOrigin: String = git("ls-remote", "--get-url", "origin")(cwd, log)

  def headCommitMessage: Option[String] = Try(git("log", "--pretty=%s\n\n%b", "-n", "1")(cwd, log)).toOption

  def changedFiles: Seq[String] =
    headCommitSha
      .flatMap(headCommit => Try(git("diff-tree", "--no-commit-id", "--name-only", "-r", headCommit)(cwd, log)).toOption)
      .map(_.split('\n').map(_.trim).toSeq)
      .getOrElse(Seq.empty)
}

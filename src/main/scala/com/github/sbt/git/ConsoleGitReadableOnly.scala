package com.github.sbt.git

import scala.util.Try

import sbt.{File, Logger}

class ConsoleGitReadableOnly(git: GitRunner, cwd: File, log: Logger) extends GitReadonlyInterface {
  private def splitOutput(output: String): Seq[String] =
    output.split("\\s+").toSeq.filter(_.nonEmpty)

  def branch: String =
    Try(git("symbolic-ref", "--short", "-q", "HEAD")(cwd, log))
      .orElse(Try(git("rev-parse", "--abbrev-ref", "HEAD")(cwd, log)))
      .getOrElse("")

  def headCommitSha: Option[String] = Try(git("rev-parse", "--verify", "--quiet", "HEAD")(cwd, log)).toOption

  def headCommitDate: Option[String] =
    headCommitSha.flatMap(_ => Try(git("log", """--pretty="%cI"""", "-n", "1")(cwd, log)).toOption)

  def currentTags: Seq[String] =
    headCommitSha
      .map(_ => Try(splitOutput(git("tag", "--points-at", "HEAD")(cwd, log))).getOrElse(Seq()))
      .getOrElse(Seq())

  def describedVersion: Option[String] =
    headCommitSha.flatMap(_ => Try(splitOutput(git("describe", "--tags")(cwd, log)).headOption).toOption.flatten)

  override def describedVersion(patterns: Seq[String]): Option[String] =
    patterns.headOption.fold(describedVersion)(pat =>
      headCommitSha.flatMap(_ => Try(splitOutput(git("describe", "--tags", "--match", pat)(cwd, log)).headOption).toOption.flatten)
    )

  def hasUncommittedChanges: Boolean = Try(!git("status", "-s", "--untracked-files=no")(cwd, log).trim.isEmpty).getOrElse(true)

  def branches: Seq[String] = Try(splitOutput(git("branch", "--list")(cwd, log))).getOrElse(Seq())

  def remoteBranches: Seq[String] = Try(splitOutput(git("branch", "-l", "--remotes")(cwd, log))).getOrElse(Seq())

  def remoteOrigin: String = git("ls-remote", "--get-url", "origin")(cwd, log)

  def headCommitMessage: Option[String] =
    headCommitSha.flatMap(_ => Try(git("log", "--pretty=%B", "-n", "1")(cwd, log)).toOption)
}

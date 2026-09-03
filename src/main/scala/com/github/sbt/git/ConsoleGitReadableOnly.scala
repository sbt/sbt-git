package com.github.sbt.git

import scala.util.Try

import sbt.{File, Logger}

class ConsoleGitReadableOnly(git: GitRunner, cwd: File, log: Logger) extends GitReadonlyInterface {
  private def splitOutput(output: String): Seq[String] =
    output.split("\\s+").toSeq.filter(_.nonEmpty)

  private def describeArgs(patterns: Seq[String]): Seq[String] =
    Seq("describe", "--tags") ++ patterns.flatMap(Seq("--match", _))

  def branch: String =
    Try(git("symbolic-ref", "--short", "-q", "HEAD")(cwd, log))
      .orElse(Try(git("rev-parse", "--abbrev-ref", "HEAD")(cwd, log)))
      .getOrElse("")

  def headCommitSha: Option[String] = Try(git("rev-parse", "--verify", "--quiet", "HEAD")(cwd, log)).toOption

  def headCommitDate: Option[String] =
    headCommitSha.flatMap(_ => Try(git("log", "--date=format:%Y-%m-%dT%H:%M:%S%z", "--pretty=%cd", "-n", "1")(cwd, log)).toOption)

  def currentTags: Seq[String] =
    headCommitSha
      .map(_ => Try(splitOutput(git("tag", "--points-at", "HEAD")(cwd, log))).getOrElse(Seq()))
      .getOrElse(Seq())

  def describedVersion: Option[String] =
    headCommitSha.flatMap(_ => Try(splitOutput(git("describe", "--tags")(cwd, log)).headOption).toOption.flatten)

  override def describedVersion(patterns: Seq[String]): Option[String] =
    patterns.headOption.fold(describedVersion)(_ =>
      headCommitSha.flatMap(_ => Try(splitOutput(git(describeArgs(patterns)*)(cwd, log)).headOption).toOption.flatten)
    )

  def hasUncommittedChanges: Boolean = Try(!git("status", "-s", "--untracked-files=no")(cwd, log).trim.isEmpty).getOrElse(true)

  def branches: Seq[String] = Try(splitOutput(git("branch", "--list", "--format=%(refname:short)")(cwd, log))).getOrElse(Seq())

  def remoteBranches: Seq[String] = Try(splitOutput(git("branch", "--remotes", "--format=%(refname:short)")(cwd, log))).getOrElse(Seq())

  def remoteOrigin: String = Try(git("ls-remote", "--get-url", "origin")(cwd, log)).getOrElse("origin")

  def headCommitMessage: Option[String] =
    headCommitSha.flatMap(_ => Try(git("log", "--pretty=%B", "-n", "1")(cwd, log)).toOption)
}

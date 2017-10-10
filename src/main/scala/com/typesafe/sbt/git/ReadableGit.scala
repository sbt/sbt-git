package com.typesafe.sbt.git

/** An interface for interacting with git in a read-only manner. */
trait ReadableGit {
  def withGit[A](f: GitReadonlyInterface => A): A
}

/** The read-only interface to a git repository, used to define settings. */
trait GitReadonlyInterface {
  /** The currently checked out branch. */
  def branch: String
  /** The current commit SHA of the local repository, or none. */
  def headCommitSha: Option[String]
  /** The current commit date of the local repository in ISO-8601 format, or none. */
  def headCommitDate: Option[String]
  /** The current tags associated with the local repository (at its HEAD). */
  def currentTags: Seq[String]
  /** Version of the software as returned by `git describe --tags`. */
  def describedVersion: Option[String]
  /** Whether there are uncommitted changes (i.e. whether any tracked file has changed) */
  def hasUncommittedChanges: Boolean
  /** The local branches */
  def branches : Seq[String]
  /** The remote branches */
  def remoteBranches: Seq[String]
  /** The remote origin as returned by `git ls-remote --get-url origin`. */
  def remoteOrigin: String
  /** The message of current commit **/
  def headCommitMessage: Option[String]
}


/** Our default readable git uses JGit instead of a process-forking and reading, for speed/safety. */
final class DefaultReadableGit(base: sbt.File) extends ReadableGit {
  // TODO - Should we cache git, or just create on each request?
  // For now, let's cache.
  private[this] val git = JGit(base)
  /** Use the git read-only interface. */
  def withGit[A](f: GitReadonlyInterface => A): A =
    // JGit has concurrency issues so we synchronize access to it.
    synchronized(f(git))
}

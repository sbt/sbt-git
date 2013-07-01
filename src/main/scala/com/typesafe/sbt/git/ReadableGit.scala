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
  /** The current tags associated with the local repository (at its HEAD). */
  def currentTags: Seq[String]
}


/** Our default readable git uses JGit instead of a process-forking and reading, for speed/safety. */
final class DefaultReadableGit(base: sbt.File) extends ReadableGit {
  // TODO - Should we cache git, or just create on each request?
  // For now, let's cache.
  val git = JGit(base)

  def withGit[A](f: GitReadonlyInterface => A): A   = f(git)
}
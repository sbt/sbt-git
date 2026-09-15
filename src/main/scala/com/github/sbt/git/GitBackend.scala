package com.github.sbt.git

/** Selects which Git implementation sbt-git should use.
  *
  * System Git means the `git` executable available to the sbt process,
  * normally through PATH. JGit means the Java implementation on the plugin
  * classpath.
  */
sealed trait GitBackend extends Product with Serializable

object GitBackend {
  /** Prefer the system `git` executable and fall back to JGit if it is not
    * available.
    */
  case object SystemGitFirst extends GitBackend

  /** Always use the system `git` executable. Builds fail if it is not
    * available or cannot run the requested operation.
    */
  case object SystemGitOnly extends GitBackend

  /** Always use JGit. Builds fail if JGit cannot handle the requested
    * repository layout or operation.
    */
  case object JGitOnly extends GitBackend
}

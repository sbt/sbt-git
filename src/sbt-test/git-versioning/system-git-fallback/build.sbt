enablePlugins(GitVersioning)

com.github.sbt.TestHooks.forceSystemGitUnavailable

val expectJGitFallback = taskKey[Unit]("checks SystemGitFirst falls back to JGit when system git is unavailable")
expectJGitFallback := {
  val readBackend = (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitReadBackend).value
  val operationBackend = (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitOperationBackend).value
  val runner = (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitRunner).value

  assert(readBackend == com.github.sbt.git.GitBackend.SystemGitFirst, s"Unexpected read backend: $readBackend")
  assert(operationBackend == com.github.sbt.git.GitBackend.SystemGitFirst, s"Unexpected operation backend: $operationBackend")
  assert(runner == com.github.sbt.git.JGitRunner, s"Expected JGitRunner fallback, found $runner")
}

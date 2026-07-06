enablePlugins(GitVersioning)

useJGit

val checkUseJGit = taskKey[Unit]("checks the legacy useJGit helper")

checkUseJGit := {
  val readBackend =
    (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitReadBackend).value
  val operationBackend =
    (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitOperationBackend).value
  val runner =
    (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitRunner).value

  assert(
    readBackend == com.github.sbt.git.GitBackend.JGitOnly,
    s"Expected useJGit to set gitReadBackend=JGitOnly, found $readBackend"
  )
  assert(
    operationBackend == com.github.sbt.git.GitBackend.JGitOnly,
    s"Expected useJGit to set gitOperationBackend=JGitOnly, found $operationBackend"
  )
  assert(
    runner == com.github.sbt.git.JGitRunner,
    s"Expected useJGit to select JGitRunner, found $runner"
  )
}

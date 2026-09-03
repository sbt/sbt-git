enablePlugins(GitVersioning)

val expectSystemGitFirstBackend =
  taskKey[Unit]("checks the default read backend in a linked worktree")

expectSystemGitFirstBackend := {
  val expectedReadBackend = com.github.sbt.git.GitBackend.SystemGitFirst
  val actualReadBackend =
    (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitReadBackend).value
  assert(
    actualReadBackend == expectedReadBackend,
    s"Expected gitReadBackend=$expectedReadBackend, found $actualReadBackend"
  )
}

val expectWorktreeReadableValues =
  taskKey[Unit]("checks readable git values in a linked worktree")

expectWorktreeReadableValues := {
  assert(git.gitHeadCommit.value.nonEmpty, "Expected gitHeadCommit to be defined")
  assert(
    git.gitHeadMessage.value.map(_.trim).contains("Add scripted project"),
    s"Unexpected head message: ${git.gitHeadMessage.value}"
  )
  assert(
    git.gitCurrentBranch.value == "wt",
    s"Unexpected current branch: ${git.gitCurrentBranch.value}"
  )
  assert(git.gitUncommittedChanges.value, "Expected dirty linked worktree")
}

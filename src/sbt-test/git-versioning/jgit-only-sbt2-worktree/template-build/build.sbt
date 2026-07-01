enablePlugins(GitVersioning)

ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitReadBackend := {
  if (sbtVersion.value.startsWith("2.")) com.github.sbt.git.GitBackend.JGitOnly
  else com.github.sbt.git.GitBackend.SystemGitFirst
}

val expectJGitOnlyBackend =
  taskKey[Unit]("checks the forced JGit read backend in a linked worktree")

expectJGitOnlyBackend := {
  if (sbtVersion.value.startsWith("2.")) {
    val expectedReadBackend = com.github.sbt.git.GitBackend.JGitOnly
    val actualReadBackend =
      (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.gitReadBackend).value
    assert(
      actualReadBackend == expectedReadBackend,
      s"Expected gitReadBackend=$expectedReadBackend, found $actualReadBackend"
    )
  }
}

val expectWorktreeReadableValues =
  taskKey[Unit]("checks JGit readable git values in a linked worktree")

expectWorktreeReadableValues := {
  if (sbtVersion.value.startsWith("2.")) {
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
}

def proj(name: String) = Project(name, file(name)).enablePlugins(GitVersioning)


lazy val a = proj("a")
lazy val b = proj("b")

enablePlugins(GitVersioning)

git.baseVersion := "1.0"
git.versionProperty := "DUMMY_BUILD_VERSION"

val checkChangedFiles = taskKey[Unit]("checks the files changed in the last commit")
checkChangedFiles := {
  val value = git.gitFilesChangedLastCommit.value
  assert(value sameElements Seq("README2.md", "README3.md"), s"changed files should return 2 entries, got $value")
}
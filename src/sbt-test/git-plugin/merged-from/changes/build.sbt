def proj(name: String) = Project(name, file(name)).enablePlugins(GitVersioning)


lazy val a = proj("a")
lazy val b = proj("b")

enablePlugins(GitVersioning)

git.baseVersion := "1.0"
git.versionProperty := "DUMMY_BUILD_VERSION"
git.gitMergeMessagePatterns := Seq(
  raw"Merge branch '(.*?)'"
)

val checkMergedFrom = taskKey[Unit]("checks the merged from branch is correct")
checkMergedFrom := {
  val value = git.gitMergeFrom.value
  assert(value == Option("branch_2"), s"Merged from should return the correct branch, got $value")
}
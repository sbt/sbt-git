def proj(name: String) = Project(name, file(name)).enablePlugins(GitVersioning)


lazy val a = proj("a")
lazy val b = proj("b")

enablePlugins(GitVersioning)

git.baseVersion := "1.0"
git.versionProperty := "DUMMY_BUILD_VERSION"

val checkVersion = taskKey[Unit]("checks the version is the correct versino")
checkVersion := {
  val v = (a / version).value
  val v2 = (b / version).value
  assert(v == v2, s"multi-module projects should all share the same verison.  $v != $v2")
}
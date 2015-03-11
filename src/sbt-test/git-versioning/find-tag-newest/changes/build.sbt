import complete.DefaultParsers._

enablePlugins(GitVersioning)

git.baseVersion := "0.1"
git.versionProperty := "DUMMY_BUILD_VERSION"

val checkVersion = inputKey[Unit]("checks the version is the tag versino")
checkVersion := {
  val Seq(v) = spaceDelimited("<arg>").parsed
  val v2 = version.value
  assert(v == v2, s"Expected version is wrong, found ${v2}, expected ${v}")
}
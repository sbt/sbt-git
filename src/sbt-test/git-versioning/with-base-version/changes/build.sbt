enablePlugins(GitVersioning)

git.baseVersion := "1.0"
git.versionProperty := "DUMMY_BUILD_VERSION"

val checkVersion = taskKey[Unit]("checks the version is the correct versino")
checkVersion := {
  val v = version.value
  assert(v startsWith "1.0", s"git.baseVersion is meant to be optional ${v}")
  assert(isSnapshot.value == true, "isSnapshot should be true if not on a tag.")
}
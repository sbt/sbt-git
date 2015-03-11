enablePlugins(GitVersioning)

git.baseVersion := "0.1"
git.versionProperty := "DUMMY_BUILD_VERSION"

val checkVersion = taskKey[Unit]("checks the version is the tag versino")
checkVersion := {
  val v = version.value
  val v2 = (version in ThisBuild).value
  val tags = git.gitCurrentTags.value
  assert(tags == Seq("v1.0.0"), s"Failed to discover git tag, tags: $tags")
  assert(v2 == "1.0.0", s"Failed to detect git tag, found ${v}")
  assert(v == "1.0.0", s"Version from ThisBuild not used, found ${v}")
}
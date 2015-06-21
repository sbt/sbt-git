import complete.DefaultParsers._

enablePlugins(GitVersioning)

git.baseVersion := "1.0"
git.versionProperty := "DUMMY_BUILD_VERSION"
git.useGitDescribe := true

val checkVersionStartsWith = inputKey[Unit]("checks the version starts with what it should")
checkVersionStartsWith := {
  val Seq(expectedV) = spaceDelimited("<arg>").parsed
  val v = version.value
  assert(v startsWith expectedV, s"version should start with ${expectedV}. version is ${v}")
}

val checkSnapshotVersion = taskKey[Unit]("checks the version is a snapshot version")
checkSnapshotVersion := {
  val v = version.value
  assert(git.gitUncommittedChanges.value, s"Should detect uncommitted git changes.")
  assert(v endsWith "-SNAPSHOT", s"Should have -SNAPSHOT appended when uncommitted changes. ${v}")
  assert(isSnapshot.value == true, "-SNAPSHOT versions should have isSnapshot true.")
}
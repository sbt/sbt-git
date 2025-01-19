val commonSettings = Seq(
  git.useGitDescribe := true,
  git.gitDescribePatterns := Seq(s"${name.value}-*"),
  git.gitTagToVersionNumber := { tag =>
    if (tag matches s"${name.value}-[0-9].*") Some(tag.replace(s"${name.value}-", ""))
    else None
  }
)
def proj(name: String) = Project(name, file(name)).enablePlugins(GitVersioning).settings(commonSettings)

lazy val a = proj("a")
lazy val b = proj("b")

enablePlugins(GitVersioning)

git.baseVersion := "1.0"
git.versionProperty := "DUMMY_BUILD_VERSION"

val checkVersion = taskKey[Unit]("checks the version is the correct versino")
checkVersion := {
  val v = (a / version).value
  val v2 = (b / version).value
  assert(v.startsWith("1.0-1-"), s"multi-module projects should get a version with the matching git describe pattern. $v does not match '1.0-1-'")
  assert(v2 == "2.0", s"multi-module projects should get a version with the matching git describe pattern. $v2 != 2.0")
}

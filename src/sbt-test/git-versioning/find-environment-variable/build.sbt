enablePlugins(GitVersioning)

val checkVersion = taskKey[Unit]("checks the version is the tag versino")
checkVersion := {
  val v = version.value
  val prop = sys.props("project.version")
  assert(v == prop, s"Failed to set version to environment variable.  Found: $v, Expected: $prop")
}
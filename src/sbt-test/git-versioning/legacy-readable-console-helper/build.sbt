enablePlugins(GitVersioning)

useReadableConsoleGit

val checkUseReadableConsoleGit =
  taskKey[Unit]("checks the legacy useReadableConsoleGit helper")

checkUseReadableConsoleGit := {
  val forceConsoleReads =
    (ThisBuild / com.github.sbt.git.SbtGit.GitKeys.useConsoleForROGit).value

  assert(
    forceConsoleReads,
    "Expected useReadableConsoleGit to set the legacy console read flag"
  )
}

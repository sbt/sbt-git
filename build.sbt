sbtPlugin := true

name := "sbt-git"
organization := "com.typesafe.sbt"


enablePlugins(GitVersioning)
git.baseVersion := "0.8"


libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.7.0.201502260915-r",
  "org.slf4j" % "slf4j-nop" % "1.7.13" // explicitly forcing the NO-OP binder to avoid warnings to be printed
)

publishMavenStyle := false


scriptedSettings
scriptedLaunchOpts += s"-Dproject.version=${version.value}"

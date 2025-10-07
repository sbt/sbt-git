organization := "com.github.sbt"
name := "sbt-git"
licenses := Seq(("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause")))
description := "An sbt plugin that offers git features directly inside sbt"
developers := List(Developer("jsuereth", "Josh Suereth", "joshua suereth gmail com", url("http://jsuereth.com/")))
startYear := Some(2011)
homepage := scmInfo.value map (_.browseUrl)
scmInfo := Some(ScmInfo(url("https://github.com/sbt/sbt-git"), "scm:git:git@github.com:sbt/sbt-git.git"))

lazy val scala212 = "2.13.17"
lazy val scala3 = "3.7.3"

crossScalaVersions := Seq(scala212, scala3)

enablePlugins(GitVersioning, SbtPlugin)
git.baseVersion := "1.0"

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.3.202401111512-r",
  "com.michaelpollmeier" % "versionsort" % "1.0.17",
  "org.scalameta" %% "munit" % "1.2.0" % Test
)

(pluginCrossBuild / sbtVersion) := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.5.8"
    case _ => "2.0.0-RC3"
  }
}

scriptedLaunchOpts += s"-Dproject.version=${version.value}"

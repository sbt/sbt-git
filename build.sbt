organization := "com.typesafe.sbt"
sonatypeProfileName := "com.typesafe"
name := "sbt-git"
licenses := Seq(("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause")))
description := "An sbt plugin that offers git features directly inside sbt"
developers := List(Developer("jsuereth", "Josh Suereth", "joshua suereth gmail com", url("http://jsuereth.com/")))
startYear := Some(2011)
homepage := scmInfo.value map (_.browseUrl)
scmInfo := Some(ScmInfo(url("https://github.com/sbt/sbt-git"), "scm:git:git@github.com:sbt/sbt-git.git"))

crossSbtVersions := List("1.3.13")

enablePlugins(GitVersioning, SbtPlugin)
git.baseVersion := "1.0"

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.11.1.202105131744-r",
  "com.michaelpollmeier" % "versionsort" % "1.0.0",
  "org.scalameta" %% "munit" % "0.7.29" % Test
)

scriptedLaunchOpts += s"-Dproject.version=${version.value}"

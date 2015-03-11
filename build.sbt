sbtPlugin := true

name := "sbt-git"
organization := "com.typesafe.sbt"
version := "0.7.1-SNAPSHOT"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.7.0.201502260915-r"

publishMavenStyle := false


scriptedSettings
scriptedLaunchOpts += s"-Dproject.version=${version.value}"
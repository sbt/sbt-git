sbtPlugin := true

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

name := "sbt-git-plugin"

organization := "com.typesafe"

version := "0.1"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "1.1.0.201109151100-r"

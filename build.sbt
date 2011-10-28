sbtPlugin := true

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

name := "sbt-git-plugin"

organization := "com.jsuereth"

version := "0.1"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "1.1.0.201109151100-r"

publishTo <<= (version) { v =>
  import Classpaths._
  Option(if (v endsWith "SNAPSHOT") typesafeSnapshots else typesafeResolver)
}

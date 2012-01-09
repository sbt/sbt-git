sbtPlugin := true

// This should be tied to sbtPlugin IMHO.
publishMavenStyle := false

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

name := "sbt-git-plugin"

organization := "com.jsuereth"

version := "0.4"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "1.1.0.201109151100-r"

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))


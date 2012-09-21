sbtPlugin := true

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

name := "sbt-git"

organization := "com.typesafe.sbt"

version := "0.5.1-SNAPSHOT"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "1.1.0.201109151100-r"

publishMavenStyle := false

publishTo <<= (version) { v =>
  def scalasbt(repo: String) = ("scalasbt " + repo, "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-" + repo)
  val (name, repo) = if (v.endsWith("-SNAPSHOT")) scalasbt("snapshots") else scalasbt("releases")
  Some(Resolver.url(name, url(repo))(Resolver.ivyStylePatterns))
}

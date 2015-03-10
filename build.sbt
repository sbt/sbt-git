sbtPlugin := true

name := "sbt-git"

organization := "com.typesafe.sbt"

version := "0.7.0"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.7.0.201502260915-r"

publishMavenStyle := false

publishTo := {
  def scalasbt(repo: String) = ("scalasbt " + repo, "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-" + repo)
  val (name, repo) = if (isSnapshot.value) scalasbt("snapshots") else scalasbt("releases")
  Some(Resolver.url(name, url(repo))(Resolver.ivyStylePatterns))
}

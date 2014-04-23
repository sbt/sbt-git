sbtPlugin := true

name := "sbt-git"

organization := "com.typesafe.sbt"

version := "0.6.4-SNAPSHOT"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.3.1.201403241930-r"

publishMavenStyle := false

publishTo := {
  def scalasbt(repo: String) = ("scalasbt " + repo, "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-" + repo)
  val (name, repo) = if (isSnapshot.value) scalasbt("snapshots") else scalasbt("releases")
  Some(Resolver.url(name, url(repo))(Resolver.ivyStylePatterns))
}

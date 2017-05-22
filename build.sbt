organization := "com.typesafe.sbt"
name := "sbt-git"
licenses := Seq(("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause")))
description := "An sbt plugin that offers git features directly inside sbt"
developers := List(Developer("jsuereth", "Josh Suereth", "joshua suereth gmail com", url("http://jsuereth.com/")))
startYear := Some(2011)
homepage := scmInfo.value map (_.browseUrl)
scmInfo := Some(ScmInfo(url("https://github.com/sbt/sbt-git"), "scm:git:git@github.com:sbt/sbt-git.git"))

sbtPlugin := true

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.0.201609210915-r"
)

scriptedSettings
scriptedLaunchOpts += s"-Dproject.version=${version.value}"

// Release
import ReleaseTransformations._

// see https://github.com/sbt/sbt-release/issues/59
val updateReadmeVersion: ReleaseStep = { s: State =>
  val contents = IO.read(file("README.md"))

  val p = Project.extract(s)

  val pattern = "(\"" + p.get(organization) + "\"\\s+%+\\s+\"" + p.get(name) + "\"\\s+%\\s+\")[\\w\\.-]+(\")"

  val x = p.get(releaseVersion)
  val newContents = contents.replaceAll(pattern, "$1" + p.get(releaseVersion) + "$2")
  IO.write(file("README.md"), newContents)

  s
}

def insertBeforeIn(seq: Seq[ReleaseStep], before: ReleaseStep, step: ReleaseStep) = {
  val (beforeStep, rest) =
    seq.span(_ != before)

  (beforeStep :+ step) ++ rest
}

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  updateReadmeVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  releaseStepTask(bintrayRelease in This),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

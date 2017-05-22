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
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import ReleaseTransformations._

import scala.io.{Codec, Source}

val updateReadmeVersion = ReleaseStep { state =>
  val vcs = Project.extract(state).get(releaseVcs).getOrElse {
    sys.error("VCS not set")
  }
  val (releaseVer, _) = state.get(ReleaseKeys.versions).getOrElse {
    sys.error(s"${ReleaseKeys.versions.label} key not set")
  }

  val baseDir = Project.extract(state).get(baseDirectory.in(ThisBuild))
  val readmeFile = baseDir / "README.md"

  val lines = Source.fromFile(readmeFile)(Codec.UTF8).getLines().toList

  val markerText = "current version of sbt-git"
  val lineNumberOfMarker = lines.indexWhere(_.contains(markerText))

  if(lineNumberOfMarker == -1){
    throw new RuntimeException(s"Could not find marker '$markerText' in file '${readmeFile.getPath}'")
  }

  val newLine = s"""    addSbtPlugin(\"com.typesafe.sbt\" % \"sbt-git\" % "$releaseVer""""
  val newContent = lines.updated(lineNumberOfMarker + 1, newLine).mkString("\n")

  Files.write(readmeFile.toPath, newContent.getBytes(StandardCharsets.UTF_8))
  vcs.add(readmeFile.getAbsolutePath).!!(state.log)

  state
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

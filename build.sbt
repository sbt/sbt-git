organization := "com.typesafe.sbt"
name := "sbt-git"
licenses := Seq(("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause")))
description := "An sbt plugin that offers git features directly inside sbt"
developers := List(Developer("jsuereth", "Josh Suereth", "joshua suereth gmail com", url("http://jsuereth.com/")))
startYear := Some(2011)
homepage := scmInfo.value map (_.browseUrl)
scmInfo := Some(ScmInfo(url("https://github.com/sbt/sbt-git"), "scm:git:git@github.com:sbt/sbt-git.git"))

sbtPlugin := true

enablePlugins(GitVersioning)
git.baseVersion := "0.9"

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.0.201609210915-r"
)

scriptedSettings2
scriptedLaunchOpts += s"-Dproject.version=${version.value}"

// WORKAROUND https://github.com/sbt/sbt/issues/3325
def scriptedSettings2 = Def settings (
  scriptedSettings filterNot (_.key.key.label == libraryDependencies.key.label),
  libraryDependencies ++= {
    val cross = CrossVersion partialVersion scriptedSbt.value match {
      case Some((0, 13)) => CrossVersion.Disabled
      case Some((1, _))  => CrossVersion.binary
      case _             => sys error s"Unhandled sbt version ${scriptedSbt.value}"
    }
    Seq(
      "org.scala-sbt" % "scripted-sbt" % scriptedSbt.value % scriptedConf.toString cross cross,
      "org.scala-sbt" % "sbt-launch" % scriptedSbt.value % scriptedLaunchConf.toString
    )
  }
)

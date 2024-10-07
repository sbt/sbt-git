val sbtGit = project
  .in(file("."))
  .enablePlugins(SbtPlugin)
  //TODO: enablePlugins(GitVersioning, SbtPlugin)
  .settings(
    organization := "com.github.sbt",
    //sonatypeProfileName := "com.github.sbt",
    name := "sbt-git",
    licenses := Seq(
      ("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause"))
    ),
    description := "An sbt plugin that offers git features directly inside sbt",
    developers := List(
      Developer(
        "jsuereth",
        "Josh Suereth",
        "joshua suereth gmail com",
        url("http://jsuereth.com/")
      )
    ),
    startYear := Some(2011),
    homepage := scmInfo.value map (_.browseUrl),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/sbt/sbt-git"),
        "scm:git:git@github.com:sbt/sbt-git.git"
      )
    ),
//crossSbtVersions := List("1.3.13"),
//git.baseVersion := "1.0",
    libraryDependencies ++= Seq(
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.3.202401111512-r",
      "com.michaelpollmeier" % "versionsort" % "1.0.11",
      "org.scalameta" %% "munit" % "1.0.2" % Test
    ),
    // [error] (Compile / doc) java.lang.ClassNotFoundException: dotty.tools.dottydoc.Main
    packageDoc / publishArtifact := false,
    crossScalaVersions := Seq("2.12.20", "3.3.4"),
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.3.0"
        case _      => "2.0.0-M2"
      }
    },
    scriptedLaunchOpts += s"-Dproject.version=${version.value}"
  )

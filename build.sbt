organization := "com.typesafe.sbt"
name := "sbt-git"

sbtPlugin := true

enablePlugins(GitVersioning)
git.baseVersion := "0.9"

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "4.5.0.201609210915-r"
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

scriptedSettings
scriptedLaunchOpts += s"-Dproject.version=${version.value}"

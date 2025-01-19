val location = file("..").toURI

val sbtGit = RootProject(location)

val root = project.in(file(".")).dependsOn(sbtGit)

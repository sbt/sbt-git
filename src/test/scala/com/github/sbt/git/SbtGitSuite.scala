package com.github.sbt.git

import sbt.ScmInfo
import sbt.url

class SbtGitSuite extends munit.FunSuite {
  val expectedScmInfo = Some(
    ScmInfo(
      browseUrl = url("https://github.com/akka/akka"),
      connection = "scm:git:https://github.com/akka/akka.git",
      devConnection = Some("scm:git:git@github.com:akka/akka.git")
    )
  )

  test("a git URL with the .git postfix") {
    assertEquals(SbtGit.parseScmInfo("git@github.com:akka/akka.git"), expectedScmInfo)
  }
  test("a git URL without the .git postfix") {
    assertEquals(SbtGit.parseScmInfo("git@github.com:akka/akka"), expectedScmInfo)
  }
  test("a https URL with the .git postfix") {
    assertEquals(SbtGit.parseScmInfo("https://github.com/akka/akka.git"), expectedScmInfo)
  }
  test("a https URL without the .git postfix") {
    assertEquals(SbtGit.parseScmInfo("https://github.com/akka/akka"), expectedScmInfo)
  }
}

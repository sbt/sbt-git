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

  test("SystemGitFirst uses system git only when it is available") {
    assert(SbtGit.useSystemGit(GitBackend.SystemGitFirst, systemGitAvailable = true))
    assertEquals(SbtGit.useSystemGit(GitBackend.SystemGitFirst, systemGitAvailable = false), false)
  }

  test("SystemGitOnly always selects system git") {
    assert(SbtGit.useSystemGit(GitBackend.SystemGitOnly, systemGitAvailable = true))
    assert(SbtGit.useSystemGit(GitBackend.SystemGitOnly, systemGitAvailable = false))
  }

  test("JGitOnly never selects system git") {
    assertEquals(SbtGit.useSystemGit(GitBackend.JGitOnly, systemGitAvailable = true), false)
    assertEquals(SbtGit.useSystemGit(GitBackend.JGitOnly, systemGitAvailable = false), false)
  }

  test("legacy read-only console flag forces system git reads") {
    assert(SbtGit.useSystemGitForReads(forceSystemGit = true, GitBackend.JGitOnly, systemGitAvailable = false))
  }

  test("system git availability override takes precedence over detection") {
    assert(SbtGit.detectedSystemGitAvailable(Some(true), sbt.file("."), sbt.util.Logger.Null))
    assertEquals(SbtGit.detectedSystemGitAvailable(Some(false), sbt.file("."), sbt.util.Logger.Null), false)
  }

  test("operation backend selects the expected runner") {
    assertEquals(SbtGit.selectGitRunner(GitBackend.SystemGitFirst, systemGitAvailable = true), ConsoleGitRunner)
    assertEquals(SbtGit.selectGitRunner(GitBackend.SystemGitFirst, systemGitAvailable = false), JGitRunner)
    assertEquals(SbtGit.selectGitRunner(GitBackend.SystemGitOnly, systemGitAvailable = false), ConsoleGitRunner)
    assertEquals(SbtGit.selectGitRunner(GitBackend.JGitOnly, systemGitAvailable = true), JGitRunner)
  }
}

package com.github.sbt.git

import sbt.ScmInfo
import sbt.IO
import sbt.io.syntax.*
import sbt.url

import scala.sys.process.Process

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

  test("console reader handles a repository without commits") {
    IO.withTemporaryDirectory { dir =>
      runGit(dir, "init")

      val reader = new ConsoleGitReadableOnly(ConsoleGitRunner, dir, sbt.util.Logger.Null)
      assert(reader.branch.nonEmpty)
      assertEquals(reader.headCommitSha, None)
      assertEquals(reader.currentTags, Seq.empty)
      assertEquals(reader.headCommitMessage, None)
    }
  }

  test("console reader dirty check ignores untracked files") {
    IO.withTemporaryDirectory { dir =>
      runGit(dir, "init")
      runGit(dir, "config", "user.email", "test@example.com")
      runGit(dir, "config", "user.name", "Tester")
      IO.write(dir / "README.md", "clean\n")
      runGit(dir, "add", "README.md")
      runGit(dir, "commit", "--no-gpg-sign", "-m", "Initial commit")

      val reader = new ConsoleGitReadableOnly(ConsoleGitRunner, dir, sbt.util.Logger.Null)
      IO.write(dir / "build.sbt", "untracked\n")
      assertEquals(reader.hasUncommittedChanges, false)

      IO.write(dir / "README.md", "dirty\n")
      assert(reader.hasUncommittedChanges)
    }
  }

  test("console reader returns undecorated branch names") {
    IO.withTemporaryDirectory { dir =>
      initCommittedRepo(dir)
      runGit(dir, "branch", "feature")
      runGit(dir, "update-ref", "refs/remotes/origin/main", "HEAD")

      val reader = new ConsoleGitReadableOnly(ConsoleGitRunner, dir, sbt.util.Logger.Null)
      assert(reader.branches.contains("feature"))
      assert(!reader.branches.contains("*"))
      assert(reader.remoteBranches.contains("origin/main"))
      assert(!reader.remoteBranches.contains("*"))
    }
  }

  test("console reader applies all describe match patterns") {
    IO.withTemporaryDirectory { dir =>
      initCommittedRepo(dir)
      runGit(dir, "tag", "release-1.0")

      val reader = new ConsoleGitReadableOnly(ConsoleGitRunner, dir, sbt.util.Logger.Null)
      assertEquals(reader.describedVersion(Seq("ignored-*", "release-*")), Some("release-1.0"))
    }
  }

  test("console reader commit date is not shell quoted") {
    IO.withTemporaryDirectory { dir =>
      initCommittedRepo(dir)

      val reader = new ConsoleGitReadableOnly(ConsoleGitRunner, dir, sbt.util.Logger.Null)
      val date = reader.headCommitDate.getOrElse(fail("expected a commit date"))
      assert(date.matches("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{4}"""))
    }
  }

  test("console reader falls back to origin when no origin remote is configured") {
    IO.withTemporaryDirectory { dir =>
      initCommittedRepo(dir)

      val reader = new ConsoleGitReadableOnly(ConsoleGitRunner, dir, sbt.util.Logger.Null)
      assertEquals(reader.remoteOrigin, "origin")
    }
  }

  private def initCommittedRepo(dir: sbt.File): Unit = {
    runGit(dir, "init")
    runGit(dir, "config", "user.email", "test@example.com")
    runGit(dir, "config", "user.name", "Tester")
    IO.write(dir / "README.md", "initial\n")
    runGit(dir, "add", "README.md")
    runGit(dir, "commit", "--no-gpg-sign", "-m", "Initial commit")
  }

  private def runGit(dir: sbt.File, args: String*): Unit = {
    val exitCode = Process("git" +: args, dir).!
    assertEquals(exitCode, 0)
  }
}

package com.github.sbt.git

class ReleaseVersionSuite extends munit.FunSuite {
  private def release(versions: Seq[String], suffix: String = ""): Option[String] =
    SbtGit.git.releaseVersion(versions.map("v" + _), SbtGit.git.defaultTagByVersionStrategy, suffix)

  private def assertHighest(lower: String, higher: String): Unit = {
    assertEquals(release(Seq(lower, higher)), Some(higher))
    assertEquals(release(Seq(higher, lower)), Some(higher))
  }

  test("orders the SemVer precedence examples in either tag order") {
    val versions = Seq("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0")
    for {
      (higher, index) <- versions.zipWithIndex
      lower <- versions.take(index)
    } assertHighest(lower, higher)
  }

  test("compares the core before pre-release identifiers") {
    assertHighest("1.0.0", "1.0.1-alpha-1")
    assertHighest("1.9.0", "1.10.0-beta.1")
    assertHighest("1.99.99", "2.0.0-alpha")
  }

  test("compares numeric pre-release identifiers numerically and below text") {
    assertHighest("1.0.0-beta.2", "1.0.0-beta.11")
    assertHighest("1.0.0-99", "1.0.0-alpha")
    assertHighest("1.0.0-beta.2147483647", "1.0.0-beta.2147483648")
  }

  test("compares pre-release text in ASCII order and preserves hyphens within identifiers") {
    assertHighest("1.0.0-BETA", "1.0.0-alpha")
    assertHighest("1.0.0-alpha-10", "1.0.0-alpha-2")
  }

  test("ignores build metadata for precedence and keeps the first equivalent version") {
    for (versions <- Seq(Seq("1.0.0+aaa", "1.0.0+zzz"), Seq("1.0.0", "1.0.0+build.1"), Seq("1.0.0-beta.2+aaa", "1.0.0-beta.2+zzz"))) {
      assertEquals(release(versions), versions.headOption)
      assertEquals(release(versions.reverse), versions.reverse.headOption)
    }
    assertHighest("1.0.0+999", "1.0.1+1")
  }

  test("accepts short versions without normalizing the selected text") {
    assertHighest("1", "2")
    assertHighest("1.9", "1.10")
    assertHighest("1.0-alpha", "1.0")
    assertEquals(release(Seq("1.0", "1.0.0")), Some("1.0"))
    assertEquals(release(Seq("1.0.0", "1.0")), Some("1.0.0"))
  }

  test("preserves the selected metadata and appends the uncommitted suffix") {
    assertEquals(release(Seq("1.0.0", "1.1.0-rc.1+build.007"), "-SNAPSHOT"), Some("1.1.0-rc.1+build.007-SNAPSHOT"))
  }

  test("retains legacy ordering when a version is outside the SemVer parser") {
    assertHighest("1.2.3", "1.2.3.4")
    assertHighest("1.2.3.4", "1.2.4")
    assertHighest("1.0.0", "1.0.0a")
    for (versions <- Seq("1.0.0m", "1.0.0+aaa", "1.0.0+zzz").permutations)
      assertEquals(release(versions), Some("1.0.0+zzz"))
  }

  test("retains a single custom version and handles no matching tags") {
    assertEquals(SbtGit.git.releaseVersion(Seq("custom"), Some(_), "-dirty"), Some("custom-dirty"))
    assertEquals(SbtGit.git.releaseVersion(Seq("unrelated"), SbtGit.git.defaultTagByVersionStrategy, ""), None)
    assertEquals(release(Nil), None)
  }

  test("orders the versions returned by a custom tag conversion") {
    val convert: String => Option[String] = tag => Some(tag.stripPrefix("release-"))
    assertEquals(SbtGit.git.releaseVersion(Seq("release-1.0.0-beta.2", "release-1.0.0-beta.11"), convert, ""), Some("1.0.0-beta.11"))
  }
}

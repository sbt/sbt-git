package com.github.sbt

import com.github.sbt.git.SbtGit

object TestHooks {
  val forceSystemGitUnavailable =
    _root_.sbt.ThisBuild / SbtGit.GitKeys.systemGitAvailableOverride := Some(false)
}

package com.jsuereth.git

import sbt._
import Keys._

/** Keys relative to using git from SBT */
object GitKeys {
  val gitRemoteRepo = SettingKey[String]("git-remote-repo", "The remote git repository associated with this project")
  val gitBranch = SettingKey[String]("git-branch", "Target branch of a git operation")
  val gitRunner = TaskKey[GitRunner]("git-runner", "The mechanism used to run git in the current build.")
  val gitRun = InputKey[Unit]("git", "Runs a git command directly from SBT.")
}

/** This plugin has all the basic 'git' functionality for other plugins. */
object GitPlugin extends Plugin {
  import GitKeys._
  // TODO - Should we embedd everywhere like this?
  override val settings = Seq(
    gitRunner in ThisBuild := ConsoleGitRunner,
    // Input task to run git commands directly.
    gitRun in ThisBuild <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
      (argTask, baseDirectory, gitRunner in ThisBuild, streams) map { (args: Seq[String], dir: File, runner: GitRunner, s: TaskStreams) =>
        runner(args:_*)(dir, s.log)
      }
    }
  )
  /** A Predefined setting to use JGit runner for git. */
  def useJGit = gitRunner in ThisBuild := JGitRunner

  /** A holder of keys for simple config. */
  object git {
    val remoteRepo = GitKeys.gitRemoteRepo
    val branch = GitKeys.gitBranch
    val runner = GitKeys.gitRunner in ThisBuild
  }
}









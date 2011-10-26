package com.typesafe.git 

import sbt._
import Keys._

/** Keys relative to using git from SBT */
object GitKeys {
  val gitRemoteRepo = SettingKey[String]("git-remote-repo", "The remote git repository assoicated with this project") 
  val gitRunner = TaskKey[GitRunner]("git-runner", "The mechanism used to run git in the current build.")
}

/** This plugin has all the basic 'git' functionality for other plugins. */
object GitPlugin extends Plugin {
  import GitKeys._
  // TODO - Should we embedd everywhere like this?
  override val settings = Seq(
    gitRunner in ThisBuild := ConsoleGitRunner
    // TODO - add an inputtask that can run git inside SBT.
  )
  /** A Predefined setting to use JGit runner for git. */
  def useJGit = gitRunner in ThisBuild := JGitRunner

  /** A holder of keys for simple config. */
  object git {
    val remoteRepo = GitKeys.gitRemoteRepo
    val runner = GitKeys.gitRunner in ThisBuild
  }
}









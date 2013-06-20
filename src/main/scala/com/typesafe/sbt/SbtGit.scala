package com.typesafe.sbt

import sbt._
import Keys._
import git.{ ConsoleGitRunner, GitRunner, JGitRunner, NullLogger }
import scala.util.logging.ConsoleLogger
import com.typesafe.sbt.git.GitRunner

/** This plugin has all the basic 'git' functionality for other plugins. */
object SbtGit extends Plugin {
  object GitKeys {
    val gitRemoteRepo = SettingKey[String]("git-remote-repo", "The remote git repository associated with this project")
    val gitBranch = SettingKey[Option[String]]("git-branch", "Target branch of a git operation")
    val gitRunnerSetting = SettingKey[GitRunner]("git-runner", "The mechanism used to run git in the current build.")
    @deprecated
    val gitRunner = TaskKey[GitRunner]("git-runner-task", "The mechanism used to run git in the current build.")
    val gitHeadCommit = SettingKey[String]("git-head-commit", "The commit sha for the top commit of this project.")
    val gitCurrentBranch = SettingKey[String]("git-current-branch", "The current branch for this project.")
    val gitTopTag = SettingKey[Option[String]]("git-top-tag", "The latest tag associated with this commit.")
  }

  object GitCommand {
    val action: (State, Seq[String]) => State = { (state, args) =>
      val extracted = Project.extract(state)
      import extracted._
      val runner = extracted get GitKeys.gitRunnerSetting
      val dir = extracted.get(baseDirectory)
      val result = runner(args:_*)(dir, state.log)
      // TODO - Best way to print to console?
      println(result)
      state
    }

    // <arg> is the suggestion printed for tab completion on an argument
    val command: Command = Command.args("git", "<args>")(action)

    @scala.annotation.tailrec
    private def isGitRepo(dir: File): Boolean = {
      if (dir.listFiles().map(_.getName).contains(".git")) true
      else {
        val parent = dir.getParentFile
        if (parent == null) false
        else isGitRepo(parent)
      }
    }

    val prompt: State => String = { state =>
      val extracted = Project.extract(state)
      import extracted._
      val runner = extracted get GitKeys.gitRunnerSetting
      val dir = extracted get baseDirectory
      val name = extracted get Keys.name
      if (isGitRepo(dir)) {
        val branch = runner.currentBranchOrNone(dir, NullLogger) getOrElse ""
        name + "(" + branch + ")> "
      } else {
        name + "> "
      }
    }
  }

  import GitKeys._
  // Use SBT 0.12's features for advantage!
  // We store our global build settings just once.
  override val projectSettings = Seq(
    gitRunnerSetting in ThisBuild := ConsoleGitRunner,
    gitRunner in ThisBuild <<= gitRunnerSetting map identity,
    gitHeadCommit in ThisBuild <<= (baseDirectory, gitRunnerSetting) apply { (bd, git) =>
      // TODO - Figure out logging!
      git.headCommit(bd, NullLogger)
    },
    gitTopTag in ThisBuild <<= (baseDirectory, gitRunnerSetting) apply { (bd, git) =>
      git.currentTopTagOrNone(bd, NullLogger)
    },
    gitCurrentBranch in ThisBuild <<= (baseDirectory, gitRunnerSetting) apply { (bd, git) =>
      git.currentBranchOrNone(bd, NullLogger).getOrElse("")
    }
  )
  override val settings = Seq(
    // Input task to run git commands directly.
    commands += GitCommand.command ,
    shellPrompt := GitCommand.prompt

  )
  /** A Predefined setting to use JGit runner for git. */
  def useJGit: Setting[_] = gitRunnerSetting in ThisBuild := JGitRunner

  def showCurrentGitBranch: Setting[_] =
    shellPrompt := GitCommand.prompt

  /** A holder of keys for simple config. */
  object git {
    val remoteRepo = GitKeys.gitRemoteRepo
    val branch = GitKeys.gitBranch
    val runner = GitKeys.gitRunner in ThisBuild
    val gitHeadCommit = GitKeys.gitHeadCommit in ThisBuild
    val gitTopTag = GitKeys.gitTopTag in ThisBuild
    val gitCurrentBranch = GitKeys.gitCurrentBranch in ThisBuild
  }
}

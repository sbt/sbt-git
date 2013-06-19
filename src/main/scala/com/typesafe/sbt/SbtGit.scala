package com.typesafe.sbt

import sbt._
import Keys._
import git.{ ConsoleGitRunner, GitRunner, JGitRunner }

/** This plugin has all the basic 'git' functionality for other plugins. */
object SbtGit extends Plugin {
  object GitKeys {
    val gitRemoteRepo = SettingKey[String]("git-remote-repo", "The remote git repository associated with this project")
    val gitBranch = SettingKey[Option[String]]("git-branch", "Target branch of a git operation")
    val gitRunner = TaskKey[GitRunner]("git-runner", "The mechanism used to run git in the current build.")
  }

  object GitCommand {
    val action: (State, Seq[String]) => State = { (state, args) =>
      val extracted = Project.extract(state)
      import extracted._
      val (state1, runner) = runTask(GitKeys.gitRunner, state)
      val dir = extracted.get(baseDirectory)
      val result = runner(args:_*)(dir, state1.log)
      // TODO - Best way to print to console?
      println(result)
      state1
    }

    // <arg> is the suggestion printed for tab completion on an argument
    val command: Command = Command.args("git", "<args>")(action)

    @scala.annotation.tailrec
    private def isGitRepo(dir: File): Boolean = {
      if (dir.listFiles().map(_.getName).contains(".git")) {
        true
      } else {
        val parent = dir.getParentFile
        if (parent == null) {
          false
        } else {
          isGitRepo(parent)
        }
      }
    }

    val prompt: State => String = { state =>
      val extracted = Project.extract(state)
      import extracted._
      val (state1, runner) = runTask(GitKeys.gitRunner, state)
      val dir = extracted.get(baseDirectory)
      if (isGitRepo(dir)) {
        runner.prompt(state1)(dir, state1.log)
      } else {
        "> "
      }
    }
  }

  import GitKeys._
  // TODO - Should we embedd everywhere like this?
  override val settings = Seq(
    gitRunner in ThisBuild := ConsoleGitRunner,
    // Input task to run git commands directly.
    commands += GitCommand.command,
    shellPrompt := GitCommand.prompt
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

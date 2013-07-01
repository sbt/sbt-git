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
    val gitCurrentTags = SettingKey[Seq[String]]("git-current-tags", "The tags associated with this commit.")
    val gitTagToVersionNumber = SettingKey[String => Option[String]]("git-tag-to-version-number", "Converts a git tag string to a version number.")
    val baseVersion = SettingKey[String]("base-version", "The base version number which we will append the git version to.")
    val versionProperty = SettingKey[String]("version-property", "The system property that can be used to override the version number.  Defaults to `project.version`.")
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
    gitCurrentTags in ThisBuild <<= (baseDirectory, gitRunnerSetting) apply { (bd, git) =>
      git.currentTags(bd, NullLogger)
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


  def versionWithGit: Seq[Setting[_]] =
    Seq(
        gitTagToVersionNumber in ThisBuild := (git.defaultTagByVersionStrategy _),
        baseVersion in ThisBuild := "1.0",
        versionProperty in ThisBuild := "project.version",
        version in ThisBuild <<= (git.versionProperty, git.baseVersion, git.gitHeadCommit, git.gitCurrentTags, git.gitTagToVersionNumber) apply git.makeVersion
    )


  /** A holder of keys for simple config. */
  object git {
    val remoteRepo = GitKeys.gitRemoteRepo
    val branch = GitKeys.gitBranch
    val runner = GitKeys.gitRunner in ThisBuild
    val gitHeadCommit = GitKeys.gitHeadCommit in ThisBuild
    val gitCurrentTags = GitKeys.gitCurrentTags in ThisBuild
    val gitCurrentBranch = GitKeys.gitCurrentBranch in ThisBuild
    val gitTagToVersionNumber = GitKeys.gitTagToVersionNumber in ThisBuild
    val baseVersion = GitKeys.baseVersion in ThisBuild
    val versionProperty = GitKeys.versionProperty in ThisBuild

    def defaultTagByVersionStrategy(tag: String): Option[String] = {
      if(tag matches "v[0-9].*") Some(tag drop 1)
      else None
    }
    // Simple fall-through on how to define the project version.
    def makeVersion(versionProperty: String, baseVersion: String, headCommit: String, currentTags: Seq[String], releaseTagVersion: String => Option[String]): String = {
      def releaseVersion: Option[String] = {
        val releaseVersions =
          for {
            tag <- currentTags
            version <- releaseTagVersion(tag)
          } yield version
        releaseVersions.headOption
      }
      def commitVersion: String =
         baseVersion + "-" + headCommit
      def overrideVersion = Option(sys.props(versionProperty))
      //Now we fall through the potential version numbers...
      overrideVersion  orElse releaseVersion getOrElse commitVersion
    }
  }
}

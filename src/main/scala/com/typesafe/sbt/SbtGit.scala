package com.typesafe.sbt

import sbt._
import Keys._
import git.{ ConsoleGitRunner, GitRunner, JGitRunner, NullLogger }
import scala.util.logging.ConsoleLogger
import com.typesafe.sbt.git.GitRunner
import com.typesafe.sbt.git.ReadableGit
import com.typesafe.sbt.git.DefaultReadableGit

/** This plugin has all the basic 'git' functionality for other plugins. */
object SbtGit {

  object GitKeys {
    // Read-only git settings and values for use in other build settings.
    // Note: These are all grabbed using jgit currently.
    val gitReader = SettingKey[ReadableGit]("git-reader", "This gives us a read-only view of the git repository.")
    val gitBranch = SettingKey[Option[String]]("git-branch", "Target branch of a git operation")
    val gitCurrentBranch = SettingKey[String]("git-current-branch", "The current branch for this project.")
    val gitCurrentTags = SettingKey[Seq[String]]("git-current-tags", "The tags associated with this commit.")
    val gitHeadCommit = SettingKey[Option[String]]("git-head-commit", "The commit sha for the top commit of this project.")
    val gitDescribedVersion = SettingKey[Option[String]]("git-described-version", "Version as returned by `git describe --tags`.")
    val gitUncommittedChanges = SettingKey[Boolean]("git-uncommitted-changes", "Whether there are uncommitted changes.")
    
    // A Mechanism to run Git directly.
    val gitRunner = TaskKey[GitRunner]("git-runner", "The mechanism used to run git in the current build.")

    // Keys associated with setting a version number.
    val useGitDescribe = SettingKey[Boolean]("use-git-describe", "Get version by calling `git describe` on the repository")
    val gitTagToVersionNumber = SettingKey[String => Option[String]]("git-tag-to-version-number", "Converts a git tag string to a version number.")

    // Component version strings.  We use these when determining the actual version.
    val formattedShaVersion = settingKey[Option[String]]("Completely formmated version string which will use the git SHA. Override this to change how the SHA version is formatted.")
    val formattedDateVersion = settingKey[String]("Completely formatted version string which does not rely on git.  Used as a fallback.")

    // Helper suffix/prefix information for generated default version strings.
    val baseVersion = SettingKey[String]("base-version", "The base version number which we will append the git version to.")
    val versionProperty = SettingKey[String]("version-property", "The system property that can be used to override the version number.  Defaults to `project.version`.")
    val uncommittedSignifier = SettingKey[Option[String]]("uncommitted-signifier", "Optional additional signifier to signify uncommitted changes")
    
    // The remote repository we're using.
    val gitRemoteRepo = SettingKey[String]("git-remote-repo", "The remote git repository associated with this project")
  }

  object GitCommand {
    import complete._
    import complete.DefaultParsers._

    val action: (State, Seq[String]) => State = { (state, args) =>
      val extracted = Project.extract(state)
      import extracted._
      val (state2, runner) = extracted.runTask(GitKeys.gitRunner, state)
      val dir = extracted.get(baseDirectory)
      val result = runner(args:_*)(dir, state2.log)
      state2
    }

    // the git command we expose to the user
    val command: Command = Command("git")(s =>  fullCommand(s)){ (state, arg) =>
      val (command, args) = arg
      action(state, command +: args)
    }

    // the parser providing auto-completion for git command
    // Note: This isn't an exact parser for git, it just tries to make it more convenient in sbt with a modicum of autocomplete.
    // Ideally we'd use the bash autocompletion scripts or zsh ones for full and complete information, but this actually
    // gives us a lot of bang for the buck.
    def fullCommand(state: State) = {
      val extracted = Project.extract(state)
      import extracted._
      val reader = extracted.get(GitKeys.gitReader)
      implicit val branches: Seq[String] = reader.withGit(_.branches) ++ reader.withGit(_.remoteBranches) :+ "HEAD"
      // let's not forget the user can define its own git commands and aliases so we don't want to parse the command
      // TODO we could though provide a list of available git commands
      // TODO some git commands like add take filepaths as arguments
      token(Space) ~> token(NotQuoted, "<command>") ~ (Space ~> token(branch)).*
    }

    def branch(implicit branches: Seq[String]): Parser[String] = NotQuoted.examples(branches.toSet)

    private def isGitRepo(dir: File): Boolean = {
      if (System.getenv("GIT_DIR") != null) true
      else isGitDir(dir)
    }

    @scala.annotation.tailrec
    private def isGitDir(dir: File): Boolean = {
      if (dir.listFiles().map(_.getName).contains(".git")) true
      else {
        val parent = dir.getParentFile
        if (parent == null) false
        else isGitDir(parent)
      }
    }

    val prompt: State => String = { state =>
      val extracted = Project.extract(state)
      import extracted._
      val reader = extracted get GitKeys.gitReader
      val dir = extracted get baseDirectory
      val name = extracted get Keys.name
      if (isGitRepo(dir)) {
        val branch = reader.withGit(_.branch)
        name + "(" + branch + ")> "
      } else {
        name + "> "
      }
    }
  }

  // Build settings.
  import GitKeys._
  def buildSettings = Seq(
    gitReader := new DefaultReadableGit(baseDirectory.value),
    gitRunner := ConsoleGitRunner,
    gitHeadCommit := gitReader.value.withGit(_.headCommitSha),
    gitDescribedVersion := gitReader.value.withGit(_.describedVersion),
    gitCurrentTags := gitReader.value.withGit(_.currentTags),
    gitCurrentBranch := Option(gitReader.value.withGit(_.branch)).getOrElse(""),
    gitUncommittedChanges in ThisBuild := gitReader.value.withGit(_.hasUncommittedChanges)
  )
  val projectSettings = Seq(
    // Input task to run git commands directly.
    commands += GitCommand.command
  )

  /** A Predefined setting to use JGit runner for git. */
  def useJGit: Setting[_] = gitRunner in ThisBuild := JGitRunner

  /** Adapts the project prompt to show the current project name *and* the current git branch. */
  def showCurrentGitBranch: Setting[_] =
    shellPrompt := GitCommand.prompt


  /** Uses git to control versioning.
   *
   * Versioning runs through the following:
   *
   * 1. Looks at version-property settings, and checks the sys.props to see if this has a value.
   * 2. Looks at the project tags.  The first to match the `gitTagToVersionNumberSetting` is used to assign the version.
   * 3. if we have a head commit, we attach this to the base version setting "<base-version>.<git commit sha>"
   * 4. We append the current timestamp tot he base version: "<base-version>.<timestamp>"
   */
  def versionWithGit: Seq[Setting[_]] =
    Seq(
        gitTagToVersionNumber in ThisBuild := (git.defaultTagByVersionStrategy _),
        versionProperty in ThisBuild := "project.version",
        uncommittedSignifier in ThisBuild := Some("SNAPSHOT"),
        useGitDescribe in ThisBuild := false,
        formattedShaVersion in ThisBuild := {
          val base = git.baseVersion.?.value
          val suffix =
            git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
          git.gitHeadCommit.value map { sha =>
            git.defaultFormatShaVersion(base, sha, suffix)
          }
        },
        formattedDateVersion in ThisBuild := {
          val base = git.baseVersion.?.value
          git.defaultFormatDateVersion(base, new java.util.Date)
        },
        isSnapshot in ThisBuild := {
          git.gitCurrentTags.value.isEmpty
        },
        version in ThisBuild := {
          val base = git.baseVersion.?.value
          val overrideVersion =
            git.overrideVersion(git.versionProperty.value)
          val uncommittiedSuffix =
            git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
          val releaseVersion =
            git.releaseVersion(git.gitCurrentTags.value, git.gitTagToVersionNumber.value)
          val describedVersion =
            git.flaggedOptional(git.useGitDescribe.value, git.gitDescribedVersion.value)
          val datedVersion = formattedDateVersion.value
          val commitVersion = formattedShaVersion.value
          //Now we fall through the potential version numbers...
          git.makeVersion(Seq(
             overrideVersion,
             releaseVersion,
             describedVersion,
             commitVersion
          )) getOrElse datedVersion // For when git isn't there at all.
        }
    )

  /** A holder of keys for simple config. */
  object git {
    val remoteRepo = GitKeys.gitRemoteRepo
    val branch = GitKeys.gitBranch
    val runner = GitKeys.gitRunner in ThisBuild
    val gitHeadCommit = GitKeys.gitHeadCommit in ThisBuild
    val useGitDescribe = GitKeys.useGitDescribe in ThisBuild
    val gitDescribedVersion = GitKeys.gitDescribedVersion in ThisBuild
    val gitCurrentTags = GitKeys.gitCurrentTags in ThisBuild
    val gitCurrentBranch = GitKeys.gitCurrentBranch in ThisBuild
    val gitTagToVersionNumber = GitKeys.gitTagToVersionNumber in ThisBuild
    val baseVersion = GitKeys.baseVersion in ThisBuild
    val versionProperty = GitKeys.versionProperty in ThisBuild
    val gitUncommittedChanges = GitKeys.gitUncommittedChanges in ThisBuild
    val uncommittedSignifier = GitKeys.uncommittedSignifier in ThisBuild
    val formattedShaVersion = GitKeys.formattedShaVersion in ThisBuild
    val formattedDateVersion = GitKeys.formattedDateVersion in ThisBuild


    def defaultTagByVersionStrategy(tag: String): Option[String] = {
      if(tag matches "v[0-9].*") Some(tag drop 1)
      else None
    }

    def defaultFormatShaVersion(baseVersion: Option[String], sha:String, suffix: String):String = {
      baseVersion.map(_ +"-").getOrElse("") + sha + suffix
    }
    
    def defaultFormatDateVersion(baseVersion:Option[String], date:java.util.Date):String = {
        val df = new java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss")
        df setTimeZone java.util.TimeZone.getTimeZone("GMT")
        baseVersion.map(_ +"-").getOrElse("") + (df format (new java.util.Date))
    }

    def flaggedOptional(flag: Boolean, value: Option[String]): Option[String] =
      if(flag) value
      else None

    def makeUncommittedSignifierSuffix(hasUncommittedChanges: Boolean, uncommittedSignifier: Option[String]): String =
      flaggedOptional(hasUncommittedChanges, uncommittedSignifier).map("-" + _).getOrElse("")

    def releaseVersion(currentTags: Seq[String], releaseTagVersion: String => Option[String]): Option[String] = {
      val releaseVersions =
        for {
          tag <- currentTags
          version <- releaseTagVersion(tag)
        } yield version
      // NOTE - Selecting the last tag or the first tag should be an option.
      releaseVersions.reverse.headOption
    }
    def overrideVersion(versionProperty: String) = Option(sys.props(versionProperty))

    def makeVersion(versionPossibilities: Seq[Option[String]]): Option[String] = {
      versionPossibilities.reduce(_ orElse _)
    }
  }
}

/** The autoplugin which adapts the old sbt plugin classes into a legitimate AutoPlugin.
  *
  * This will add the ability to call git directly in the sbt shell via a command, as well as add
  * the infrastructure to read git properties.
  *
  * We keep the old SbtGit object around in an attempt not to break projects which depend on the old
  * plugin directly.
  */
object GitPlugin extends AutoPlugin {
  override def requires = sbt.plugins.CorePlugin
  override def trigger = allRequirements
  // Note: In an attempt to pretend we are binary compatible, we current add this as an after thought.
  // In 1.0, we should deprecate/move the other means of getting these values.
  object autoImport {
    val git = SbtGit.git
    def versionWithGit = SbtGit.versionWithGit
    def useJGit = SbtGit.useJGit
    def showCurrentGitBranch = SbtGit.showCurrentGitBranch
  }
  override def buildSettings: Seq[Setting[_]] = SbtGit.buildSettings
  override def projectSettings: Seq[Setting[_]] = SbtGit.projectSettings
}

/** Adapter to auto-enable git versioning.  i.e. the sbt 0.13.5+ mechanism of turning it on. */
object GitVersioning extends AutoPlugin {
  override def requires = sbt.plugins.IvyPlugin && GitPlugin
  override def projectSettings = GitPlugin.autoImport.versionWithGit
}
/** Adapter to enable the git prompt. i.e. rich prompt based on git info. */
object GitBranchPrompt extends AutoPlugin {
  override def requires = GitPlugin
  override  def projectSettings = SbtGit.showCurrentGitBranch
}

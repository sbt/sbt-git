package com.typesafe.sbt

import sbt._
import Keys._
import git.{ConsoleGitRunner, DefaultReadableGit, GitRunner, JGitRunner, ReadableGit}
import sys.process.Process

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
    val gitHeadMessage = SettingKey[Option[String]]("git-head-message", "The message for the top commit of this project.")
    val gitHeadCommitDate = SettingKey[Option[String]]("git-head-commit-date", "The commit date for the top commit of this project in ISO-8601 format.")
    val gitDescribedVersion = SettingKey[Option[String]]("git-described-version", "Version as returned by `git describe --tags`.")
    val gitUncommittedChanges = SettingKey[Boolean]("git-uncommitted-changes", "Whether there are uncommitted changes.")

    // A Mechanism to run Git directly.
    val gitRunner = TaskKey[GitRunner]("git-runner", "The mechanism used to run git in the current build.")

    // Keys associated with setting a version number.
    val useGitDescribe = SettingKey[Boolean]("use-git-describe", "Get version by calling `git describe` on the repository")
    val gitDescribePatterns = SettingKey[Seq[String]]("git-describe-patterns", "Patterns to `--match` against when using `git describe`")
    val gitTagToVersionNumber = SettingKey[String => Option[String]]("git-tag-to-version-number", "Converts a git tag string to a version number.")

    // Component version strings.  We use these when determining the actual version.
    val formattedShaVersion = settingKey[Option[String]]("Completely formatted version string which will use the git SHA. Override this to change how the SHA version is formatted.")
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
      val (state2, runner) = extracted.runTask(GitKeys.gitRunner, state)
      val dir = extracted.get(baseDirectory)
      runner(args:_*)(dir, state2.log)
      state2
    }

    // the git command we expose to the user
    val command: Command = Command("git")(s =>  fullCommand(s)){ (state, arg) =>
      val (command, args) = arg
      action(state, command +: args)
    }

    val QuotedString: Parser[String] = DQuoteClass ~> any.+.string.filter(!_.contains(DQuoteClass), _ => "Invalid quoted string") <~ DQuoteClass

    // the parser providing auto-completion for git command
    // Note: This isn't an exact parser for git, it just tries to make it more convenient in sbt with a modicum of autocomplete.
    // Ideally we'd use the bash autocompletion scripts or zsh ones for full and complete information, but this actually
    // gives us a lot of bang for the buck.
    def fullCommand(state: State) = {
      val extracted = Project.extract(state)
      val reader = extracted.get(GitKeys.gitReader)
      implicit val branches: Seq[String] = reader.withGit(_.branches) ++ reader.withGit(_.remoteBranches) :+ "HEAD"
      // let's not forget the user can define its own git commands and aliases so we don't want to parse the command
      // TODO we could though provide a list of available git commands
      // TODO some git commands like add take filepaths as arguments
      token(Space) ~> token(NotQuoted, "<command>") ~ (Space ~> token(branch | QuotedString)).*
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
    gitHeadMessage := gitReader.value.withGit(_.headCommitMessage),
    gitHeadCommitDate := gitReader.value.withGit(_.headCommitDate),
    gitTagToVersionNumber := git.defaultTagByVersionStrategy,
    gitDescribePatterns := Seq.empty[String],
    gitDescribedVersion := gitReader.value.withGit(_.describedVersion(git.gitDescribePatterns.value)).map(v => git.gitTagToVersionNumber.value(v).getOrElse(v)),
    gitCurrentTags := gitReader.value.withGit(_.currentTags),
    gitCurrentBranch := Option(gitReader.value.withGit(_.branch)).getOrElse(""),
    gitUncommittedChanges in ThisBuild := gitReader.value.withGit(_.hasUncommittedChanges),
    scmInfo := {
      val user = """(?:[^@\/]+@)?"""
      val domain = """([^\/]+)"""
      val gitPath = """(.*)\.git\/?"""
      val unauthenticated = raw"""(?:git|https?|ftps?)\:\/\/$domain\/$gitPath""".r
      val ssh = raw"""ssh\:\/\/$user$domain\/$gitPath""".r
      val headlessSSH = raw"""$user$domain:$gitPath""".r
      
      def buildScmInfo(domain: String, repo: String) = Option(
        ScmInfo(
          url(s"https://$domain/$repo"),
          s"scm:git:https://$domain/$repo.git",
          Some(s"scm:git:git@$domain:$repo.git")
        )
      )

      gitReader.value.withGit(_.remoteOrigin) match {
        case unauthenticated(domain, repo) => buildScmInfo(domain,repo)
        case ssh(domain, repo) => buildScmInfo(domain,repo)
        case headlessSSH(domain, repo) => buildScmInfo(domain,repo)
        case _ => None
      }
    }
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
          git.gitCurrentTags.value.isEmpty || git.gitUncommittedChanges.value
        },
        version in ThisBuild := {
          val overrideVersion =
            git.overrideVersion(git.versionProperty.value)
          val uncommittedSuffix =
            git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
          val releaseVersion =
            git.releaseVersion(git.gitCurrentTags.value, git.gitTagToVersionNumber.value, uncommittedSuffix)
          val describedVersion =
            git.flaggedOptional(git.useGitDescribe.value, git.describeVersion(git.gitDescribedVersion.value, uncommittedSuffix))
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
    val gitHeadMessage = GitKeys.gitHeadMessage in ThisBuild
    val gitHeadCommitDate = GitKeys.gitHeadCommitDate in ThisBuild
    val useGitDescribe = GitKeys.useGitDescribe in ThisBuild
    val gitDescribePatterns = GitKeys.gitDescribePatterns in ThisBuild
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


    val defaultTagByVersionStrategy: String => Option[String] = { tag =>
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

    def describeVersion(gitDescribedVersion: Option[String], suffix: String): Option[String] = {
      gitDescribedVersion.map(_ + suffix)
    }

    def releaseVersion(currentTags: Seq[String], releaseTagVersion: String => Option[String], suffix: String): Option[String] = {
      val releaseVersions =
        for {
          tag <- currentTags
          version <- releaseTagVersion(tag)
        } yield version + suffix
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
  override def buildSettings = GitPlugin.autoImport.versionWithGit
}
/** Adapter to enable the git prompt. i.e. rich prompt based on git info. */
object GitBranchPrompt extends AutoPlugin {
  override def requires = GitPlugin
  override  def projectSettings = SbtGit.showCurrentGitBranch
}

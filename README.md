# sbt-git #

The `sbt-git` plugin offers git command line features directly inside of sbt as
well as allowing other plugins to make use of git.


## Installation ##

Add the following to your `project/plugins.sbt` or `~/.sbt/plugins/plugins.sbt` file:

    resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.2")


### Using JGit ###

If you do not have git installed and available on your path (e.g. you use windows),
make sure your `git.sbt` or `~/.sbt/git.sbt` file looks like this:

    useJGit

Or you can type this into the prompt:

    > set useJGit
    [info] Reapplying settings...
    [info] Set current project to scala-arm (in build file:...)
    > session save
    [info] Reapplying settings...
    [info] Set current project to scala-arm (in build file:...)

This will enable a java-only GIT solution that, while not supporting all the same
commands that can be run in the standard git command line, is good enough for most
git activities.


## Versioning with Git ##

You can begin to use git to control your project versions.  To do so, simply add the

following setting to your build (not necessary for every project):

    git.baseVersion := "1.0"

    versionWithGit

The `baseVersion` setting represents the in-development version you're working on. While
a future release of the Git plugin may be able to automatically detect the version from tag,
we feel it's best to configure this version per branch.  Note, you can create a separate
`version.sbt` file for the project where you store the base version.

The git plugin will now autogenerate your version using the following rules, in order:

1. Looks at version-property setting (default to `project.version`), and checks the `sys.props` to see if this has a value.  If so, use it.
2. Looks at the project tags.  The first to match the `gitTagToVersionNumberSetting` is used to assign the version.  The default is to look for tags that begin with `v` and a number, and use the number as the version.  If we don't have a match, go to the next rule.
3. if we have a head commit (meaning this isn't a brand new repository), we attach this to the base version setting "<base-version>.<git commit sha>"
4. We append the current timestamp to the base version: "<base-version>.<timestamp>".

You can alter the tag-detection algorith using the `git.gitTagToVersionNumber` setting. For example, if we wanted to alter the default version tag detection so it does not require a "v" at the start of tags, we could add the following setting:

    git.gitTagToVersionNumber := { tag: String =>
      if(tag matches "[0.9]+\\..*") Some(tag)
      else None
    }


## Prompts ##

You can use the git plugin to add the project name + the current branch to your prompt. Simply add this setting to every project:

    showCurrentGitBranch

## Usage ##

In an sbt prompt, simply enter any git command.  e.g.

    > git status
    # On branch master
    # Changes not staged for commit:
    #   (use "git add <file>..." to update what will be committed)
    #   (use "git checkout -- <file>..." to discard changes in working directory)
    #
    #	modified:   build.sbt
    #	modified:   project/plugins/project/Build.scala
    #
    # Untracked files:
    #   (use "git add <file>..." to include in what will be committed)
    #
    #	src/site/
    no changes added to commit (use "git add" and/or "git commit -a")


## Licensing ##

This software is licensed under the BSD licenese.

# Attention! This is a fork of https://github.com/sbt/sbt-git #

We modified it to add customized verions formats, which in turns allow us to support rpm packaging.
Refrain from making modifications, and note that we are in the process of getting a pull request
to the original project.

To use in your project, you need to have this plugin build on a repo, or on local. 

To publish locally:
1. clone our stash fork (not the original github project): git clone https://philippe.pascal@stash.corp.creditkarma.com/scm/serv/sbt-git.git (with your name)
2. checkout the correct tag: cd sbt-git ; git checkout v0.6.5-CK
3. publish it locally: sbt publishLocal

Once it is published, add our special version 0.6.5-CK to your plugin.sbt like so:

    resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.5-CK")

To modify the formatter so that there is no dash (rpm doesn't like dash), add this to your build.sbt:

    git.formatShaVersion := {(baseVersion:String, sha:String) =>
      baseVersion + "." + sha 
    }

For all the other settings, refer to the original doc below:

# sbt-git #

The `sbt-git` plugin offers git command line features directly inside sbt as
well as allowing other plugins to make use of git.


## Installation ##

Add the following to your `project/plugins.sbt` or `~/.sbt/0.13/plugins/plugins.sbt` file:

    resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")


### Using JGit ###

If you do not have git installed and available on your path (e.g. you use windows),
make sure your `git.sbt` or `~/.sbt/0.13/git.sbt` file looks like this:

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
following settings to your build (not necessary for every project), in **this specific order**:

    import com.typesafe.sbt.SbtGit._

    versionWithGit

    // Optionally:
    git.baseVersion := "0.1"

The git plugin will now autogenerate your version using the following rules, in order:

1. Looks at version-property setting (default to `project.version`), and checks the `sys.props` to see if this has a value.  If so, use it.
2. Otherwise, looks at the project tags.  The first to match the `gitTagToVersionNumberSetting` is used to assign the version.  The default is to look for tags that begin with `v` and a number, and use the number as the version.
3. If no tags are found either, look at the head commit. We attach this to the `git.baseVersion` setting: "&lt;base-version&gt;.&lt;git commit sha&gt;"
4. If no head commit is present either (which means this is a brand-new repository with no commits yet), we append the current timestamp to the base version: "&lt;base-version&gt;.&lt;timestamp&gt;".

The `git.baseVersion` setting represents the in-development (upcoming) version you're working on.

You can alter the tag-detection algorithm using the `git.gitTagToVersionNumber` setting. For example, if we wanted to alter the default version tag detection so it does not require a "v" at the start of tags, we could add the following setting:

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

This software is licensed under the BSD license.

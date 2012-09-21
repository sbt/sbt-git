# sbt-git #

The `sbt-git` plugin offers git command line features directly inside of sbt as
well as allowing other plugins to make use of git.


## Installation ##

Add the following to your `project/plugins.sbt` or `~/.sbt/plugins/plugins.sbt` file:

    resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.5.0")


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

This will enabled a java-only GIT solution that, while not supporting all the same
commands that can be run in the standard git command line, is good enough for most
git activities.


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

u	# sbt-git-plugin #

The `sbt-git-plugin` offers git command line features directly inside of SBT as well as allowing other plugins to make use of git.

## Installation ##

Add the following to your `project/plugins/git.sbt` or `~/.sbt/plugins/git.sbt` file:
    
    resolvers += "scalasbt" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"
    
    addSbtPlugin("com.jsuereth" % "sbt-git-plugin" % "0.2")

### Using JGit ###

If you do not have git installed and available on your path (e.g. you use windows), make sure your `git.sbt` or `~/.sbt/git.sbt` file looks like this:
    
    useJGit

Or you can type this into the prompt:

    > set useJGit
    [info] Reapplying settings...
    [info] Set current project to scala-arm (in build file:...)
    > session save
    [info] Reapplying settings...
    [info] Set current project to scala-arm (in build file:...)

This will enabled a java-only GIT solution that, while not supporting all the same commands that can be run in the standard git command line, is good enough for most git activities.

## Usage ##

In an sbt prompt, simply enter any git command.  e.g.

    > git status
    [info] /home/jsuereth/projects/personal/scala-arm$ git status
    [info] # On branch master
    [info] # Changes not staged for commit:
    [info] #   (use "git add <file>..." to update what will be committed)
    [info] #   (use "git checkout -- <file>..." to discard changes in working directory)
    [info] #
    [info] #	modified:   build.sbt
    [info] #	modified:   project/plugins/project/Build.scala
    [info] #
    [info] # Untracked files:
    [info] #   (use "git add <file>..." to include in what will be committed)
    [info] #
    [info] #	src/site/
    [info] no changes added to commit (use "git add" and/or "git commit -a")


## Licensing ##

This software is licensed under the BSD licenese.

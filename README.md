# sbt-git-plugin #

The `sbt-git-plugin` offers git command line features directly inside of SBT as well as allowing other plugins to make use of git.

## Installation ##

Add the following to your `project/plugins/git.sbt` or `~/.sbt/plugins/git.sbt` file:
    
    addSbtPlugin("com.typesafe", "sbt-git-plugin", "0.1")

### Using JGit ###

If you do not have git installed and available on your path (e.g. you use windows), make sure your `git.sbt` or `~/.sbt/git.sbt` file looks like this:
    
    useJGit

This will enabled a java-only GIT solution that, while not supporting all the same commands that can be run in the standard git command line, is good enough for most git activities.

## Usage ##

In an sbt prompt, simply enter any git command.  e.g.

    > git diff


## Licensing ##

This software is licensed under the BSD licenese.

package com.typesafe.sbt.git

import sbt._
import Keys._
import org.eclipse.jgit.pgm.{Main=>JGit}

/** A mechanism to run GIT using the pure java JGit implementation. */
object JGitRunner extends GitRunner {
  override def apply(args: String*)(cwd: File, log: Logger = ConsoleLogger()): String = {
    // TODO -  Can we just assume the .git repo? I hope so....
    //JGit.main((Seq("--git-dir", cwd.getAbsolutePath + "/.git") ++ args).toArray)
    // Make a good ole fashioned classpath.
    getClass.getClassLoader match {
       case cl: java.net.URLClassLoader =>
         val cp = cl.getURLs map (_.getFile) mkString ":"
         // TODO - this is horrible, horrible code.  Can we do this a safe string way?
         // See if we can add the !! method to Fork.java
         object output extends java.io.OutputStream {
           val mBuf = new StringBuilder
           override def write(byte: Int) = mBuf append byte.toChar
           def value = mBuf.toString
         }
         Fork.java(None, Seq("-classpath", cp, "org.eclipse.jgit.pgm.Main") ++ args, Some(cwd), CustomOutput(output))
         val result = output.value
         log.info(result)
         result
       case _ => sys.error("Could not find classpath for JGit!")
    }

  }
  override def toString = "jgit"

}


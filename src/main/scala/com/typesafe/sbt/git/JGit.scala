package com.typesafe.sbt.git

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.{Git => PGit}
import java.io.File


class JGit(val repo: Repository) {
  val porcelain = new PGit(repo)


  def create(): Unit = repo.create()

  def branch = repo.getBranch

  def listBranches = {
    import collection.JavaConverters._
    porcelain.branchList.call.asScala
  }

  def checkoutBranch(branch: String): Unit = {
    // First, if remote branch exists, we auto-track it.
    val exists = listBranches exists (_.getName == ("refs/heads/" + branch))
    if(exists)  porcelain.checkout.setName(branch).call()
    else {
      // TODO - find upstream...
      import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode
      val upstream = "origin/" + branch
      porcelain.checkout.setCreateBranch(true).setName(branch)
                .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint(upstream).call()
    }
  }
}

object JGit {

  /** Creates a new git instance from a base directory. */
  def apply(base: File) = new JGit({
    val gitDir = new File(base, ".git")
    new FileRepositoryBuilder().setGitDir(gitDir)
      .readEnvironment() // scan environment GIT_* variables
     .findGitDir() // scan up the file system tree
     .build()
  })


  /** Clones from a given URI into a local directory of your choosing. */
  def clone(from: String, to: File, remoteName: String = "origin", cloneAllBranches: Boolean = true, bare: Boolean = false): JGit = {
    val git = PGit.cloneRepository.setURI(from).setRemote(remoteName).setBare(bare).setCloneAllBranches(cloneAllBranches).setDirectory(to).call()
    new JGit(git.getRepository)
  }
}
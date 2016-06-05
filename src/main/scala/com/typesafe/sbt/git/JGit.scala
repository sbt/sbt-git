package com.typesafe.sbt.git

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.{Git => PGit}
import java.io.File
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref

import scala.util.Try


// TODO - This class needs a bit more work, but at least it lets us use porcelain and wrap some higher-level
// stuff on top of JGit, as needed for our plugin.
final class JGit(val repo: Repository) extends GitReadonlyInterface {

  // forcing initialization of shallow commits to avoid concurrent modification issue. See issue #85
  //repo.getObjectDatabase.newReader.getShallowCommits()
  // Instead we've thrown a lock around sbt's usage of this class.

  val porcelain = new PGit(repo)

  def create(): Unit = repo.create()

  def branch: String = repo.getBranch

  private def branchesRef: Seq[Ref] = {
    import collection.JavaConverters._
    porcelain.branchList.call.asScala
  }

  def tags: Seq[Ref] = {
    import collection.JavaConverters._
    porcelain.tagList.call().asScala
  }

  def checkoutBranch(branch: String): Unit = {
    // First, if remote branch exists, we auto-track it.
    val exists = branchesRef exists (_.getName == ("refs/heads/" + branch))
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

  def headCommit: Option[ObjectId] =
    Option(repo.resolve("HEAD"))

  def headCommitSha: Option[String] =
    headCommit map (_.name)

  def currentTags: Seq[String] = {
    import collection.JavaConverters._
    for {
      hash <- headCommit.map(_.name).toSeq
      unpeeledTag <- tags
      taghash = tagHash(unpeeledTag)
      if taghash == hash
      ref = unpeeledTag.getName
      if ref startsWith "refs/tags/"
    } yield ref drop 10
  }


  def tagHash(tag: Ref) = {
    // Annotated (signed) and plain tags work differently,
    // plain ones have the null PeeledObjectId
    val peeled = repo.peel(tag)
    val id =
      if (peeled.getPeeledObjectId ne null)
        peeled.getPeeledObjectId
      else
        peeled.getObjectId
    id.getName
  }

  override def describedVersion: Option[String] = Try(Option(porcelain.describe().call())).getOrElse(None)

  override def hasUncommittedChanges: Boolean = porcelain.status.call.hasUncommittedChanges

  override def branches: Seq[String] = branchesRef.filter(_.getName.startsWith("refs/heads")).map(_.getName.drop(11))

  override def remoteBranches: Seq[String] = {
    import collection.JavaConverters._
    import org.eclipse.jgit.api.ListBranchCommand.ListMode
    porcelain.branchList.setListMode(ListMode.REMOTE).call.asScala.filter(_.getName.startsWith("refs/remotes")).map(_.getName.drop(13))
  }

  override def headCommitMessage: Option[String] = Try(Option(porcelain.log().setMaxCount(1).call().iterator().next().getFullMessage)).toOption.flatten
}

object JGit {

  /** Creates a new git instance from a base directory. */
  def apply(base: File) =
    try (new JGit({
      new FileRepositoryBuilder().findGitDir(base).build
    })) catch {
      // This is thrown if we never find the git base directory.  In that instance, we'll assume root is the base dir.
      case e: IllegalArgumentException =>
        val defaultGitDir = new File(base, ".git")
        new JGit({ new FileRepositoryBuilder().setGitDir(defaultGitDir).build()})
    }

  /** Clones from a given URI into a local directory of your choosing. */
  def clone(from: String, to: File, remoteName: String = "origin", cloneAllBranches: Boolean = true, bare: Boolean = false): JGit = {
    val git = PGit.cloneRepository.setURI(from).setRemote(remoteName).setBare(bare).setCloneAllBranches(cloneAllBranches).setDirectory(to).call()
    new JGit(git.getRepository)
  }
}

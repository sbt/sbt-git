package com.typesafe.sbt.git

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.{Git => PGit}
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}

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
    for {
      hash <- headCommit.map(_.name).toSeq
      unpeeledTag <- tags
      taghash = tagHash(unpeeledTag)
      if taghash == hash
      ref = unpeeledTag.getName
      if ref startsWith "refs/tags/"
    } yield ref drop 10
  }

  def reachableTags: Seq[String] = {
    import collection.JavaConverters._
    val tagsForHash: Map[String, Seq[String]] = hashToTags
    for {
      historicalHash <- porcelain.log().call().asScala.map(_.getId.getName).toSeq
      if tagsForHash.contains(historicalHash)
      tag <- tagsForHash(historicalHash)
    } yield tag
  }

  def hashToTags: Map[String, Seq[String]] = {
    val hashToTagNameTuples: Seq[(String, String)] = tags.map(tag => (tagHash(tag), tag.getName.replaceAll("^refs/tags/", "")))
    hashToTagNameTuples.groupBy(_._1).map { case (k,v) => (k,v.map(_._2))}
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

  override def remoteOrigin: String = {
    // same functionality as Process("git ls-remote --get-url origin").lines_!.head
    import collection.JavaConverters._
    porcelain.remoteList().call.asScala
      .filter(_.getName == "origin")
      .flatMap(_.getURIs.asScala)
      .headOption
      .map(_.toString)
      .getOrElse("origin")
  }

  override def headCommitMessage: Option[String] = Try(Option(porcelain.log().setMaxCount(1).call().iterator().next().getFullMessage)).toOption.flatten

  override def headCommitDate: Option[String] = {
    val walk = new RevWalk(repo)
    headCommit.map { id =>
      val commit = walk.parseCommit(id)
      val seconds = commit.getCommitTime.toLong
      val millis = seconds * 1000L
      val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      format.setTimeZone(commit.getCommitterIdent.getTimeZone)
      format.format(new Date(millis))
    }
  }
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

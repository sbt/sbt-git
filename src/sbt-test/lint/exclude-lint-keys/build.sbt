enablePlugins(GitVersioning)

// These keys are set, but nothing else in this build consumes them. sbt's `lintUnused` check
// would report them (and the ones the plugin itself sets) unless sbt-git excludes its own keys.
// See https://github.com/sbt/sbt-git/issues/379
git.remoteRepo := "git@github.com:sbt/sbt-git.git"
ThisBuild / git.baseVersion := "1.0"
ThisBuild / git.useGitDescribe := true

lazy val checkLintUnused = taskKey[Unit]("Checks that lintUnused does not report keys owned by sbt-git")

checkLintUnused := {
  val s = state.value
  val extracted = Project.extract(s)
  val include = extracted.get(Global / lintIncludeFilter)
  val exclude = extracted.get(Global / lintExcludeFilter)
  val reported = sbt.internal.LintUnused.lintUnused(s, include, exclude).map(_._1.key.label).toSet
  val gitKeys = Set(
    "baseVersion",
    "formattedDateVersion",
    "formattedShaVersion",
    "gitBranch",
    "gitCurrentBranch",
    "gitCurrentTags",
    "gitDescribePatterns",
    "gitDescribedVersion",
    "gitHeadCommit",
    "gitHeadCommitDate",
    "gitHeadMessage",
    "gitReader",
    "gitRemoteRepo",
    "gitTagToVersionNumber",
    "gitUncommittedChanges",
    "scmInfo",
    "uncommittedSignifier",
    "useConsoleForROGit",
    "useGitDescribe",
    "versionProperty"
  )
  val unexpected = reported.intersect(gitKeys)
  assert(unexpected.isEmpty, s"lintUnused reported sbt-git keys: ${unexpected.toSeq.sorted.mkString(", ")}")
}

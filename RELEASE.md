### releasing steps

* Wait for the latest GHA build on them main branch to succeed
* Create a [new release](https://github.com/sbt/sbt-git/releases) with tag `v1.x.y`, title `1.x.y` and a helpful description.
* Wait for GHA to do the work, artifacts should show up at https://repo1.maven.org/maven2/com/github/sbt/sbt-git_2.12_1.0/

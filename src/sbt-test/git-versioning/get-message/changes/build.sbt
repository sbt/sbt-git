enablePlugins(GitVersioning)

val expectNoMessage = taskKey[Unit]("checks that the commit message is empty")
expectNoMessage := {
  val headMessage = git.gitHeadMessage.value
  assert(headMessage.isEmpty, s"Expected head message to be empty, found ${headMessage}")
}

val expectedMessage = "test message\n" // git appears to add a newline for commit messages
val expectAMessage = taskKey[Unit]("checks that the commit message is equal to a predefined string")
expectAMessage := {
  val headMessage = git.gitHeadMessage.value
  assert(headMessage.get == expectedMessage, s"Expected head message to equal '${expectedMessage}', found ${headMessage}")
}

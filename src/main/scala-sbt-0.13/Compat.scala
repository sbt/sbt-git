package com.typesafe.sbt.git

import sbt._
import java.io.ByteArrayOutputStream
import java.io.File

object Compat {
  def forkOpt(baos: ByteArrayOutputStream, cwd: File) =
    ForkOptions(
      outputStrategy = Some(CustomOutput(baos)),
      workingDirectory = Some(cwd))
}

package com.github.sbt.git

import sbt.*
import scala.annotation.{meta, StaticAnnotation}

private[git] object PluginCompat {
  implicit class DefOp(singleton: Def.type) {
    def uncached[A1](a: A1): A1 = a
  }
  @meta.getter
  class cacheLevel(
      include: Array[String]
  ) extends StaticAnnotation
}

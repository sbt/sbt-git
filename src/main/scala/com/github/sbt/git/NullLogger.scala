package com.github.sbt.git

import sbt.LogEvent
import sbt.Level
import sbt.ControlEvent

object NullLogger extends sbt.BasicLogger {
  override def control(event: ControlEvent.Value, message: => String): Unit = ()
  override def log(level: Level.Value, message: => String): Unit = ()
  override def logAll(events: Seq[LogEvent]): Unit = ()
  override def success(message: => String): Unit = ()
  override def trace(t: => Throwable): Unit = ()
}

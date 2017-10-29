package com.viciouswalrus.autoextractor

import java.nio.file.Path

import akka.actor.ActorSystem
import com.viciouswalrus.filesystem.{DirectoryWatcherActor, FileSystemChangeHandler, WatchDirectory}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._

class DirectoryCreatedHandler(system: ActorSystem, watchedDirectories: Map[Path, Path])
  extends FileSystemChangeHandler with Logging {

  import system.dispatcher

  override def directoryCreated(watchedDirectory: Path, createdDir: Path): Unit = {
    logger.debug("New directory created: " + createdDir.toString)

    val handler = new FileCreatedHandler(system, createdDir, watchedDirectories(watchedDirectory))
    val watcher = DirectoryWatcherActor(system, handler)
    watcher ! WatchDirectory(createdDir)

    system.scheduler.scheduleOnce(30 seconds) {
      logger.debug("Stop watching: " + createdDir.toString)
      system.stop(watcher)
    }
  }

}

package com.viciouswalrus.autoextractor

import akka.actor.ActorSystem
import com.viciouswalrus.filesystem.{DirectoryWatcherActor, WatchDirectory}
import org.apache.logging.log4j.scala.Logging

object AutoExtractor extends App with Logging  {

  logger.info("Starting AutoExtractor")
  implicit val system: ActorSystem = ActorSystem("actorSystem")

  val handler = new DirectoryCreatedHandler(system, Config.watchedDirs.map(dir => dir.input -> dir.output).toMap)
  val watcher = DirectoryWatcherActor(system, handler)

  Config.watchedDirs.foreach(dir => {
    logger.info("Watching directory " + dir.input + " for new archives")
    watcher ! WatchDirectory(dir.input)
  })

  sys.addShutdownHook({
    logger.info("Shutdown called")
    system.terminate()
  })

}

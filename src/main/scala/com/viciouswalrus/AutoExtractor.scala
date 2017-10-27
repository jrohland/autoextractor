package com.viciouswalrus

import java.nio.file.Files
import java.nio.file.StandardWatchEventKinds._

import akka.actor.ActorSystem
import com.beachape.filemanagement.Messages.RegisterCallback
import com.beachape.filemanagement.MonitorActor
import org.apache.logging.log4j.scala.Logging

object AutoExtractor extends App with Logging  {

  logger.info("Starting AutoExtractor")
  implicit val system: ActorSystem = ActorSystem("actorSystem")

  private val parentDirectoriesMonitorActor = system.actorOf(MonitorActor(concurrency = 2))

  Config.watchedDirs.foreach(dir => {
    logger.info("Watching directory " + dir.input + " for new archives")

    parentDirectoriesMonitorActor ! RegisterCallback(
      event = ENTRY_CREATE,
      path = dir.input,
      callback = path => {
        if (Files.isDirectory(path)) {
          logger.debug("Detected new directory created: " + path.toString)

          val newDirectoryMonitorActor = system.actorOf(MonitorActor(concurrency = 2))
          val archiveActor = system.actorOf(ArchiveActor(path, dir.output, newDirectoryMonitorActor))

          newDirectoryMonitorActor ! RegisterCallback(
            event = ENTRY_MODIFY,
            path = path,
            callback = file => {
              if (!Files.isDirectory(file)) {
                logger.debug("Detected new file created: " + file.toString)
                val extension = file.toString.substring(file.toString.lastIndexOf('.') + 1).toLowerCase
                if (extension.startsWith("r")) {
                  logger.debug("Rar file detected: " + file)
                  archiveActor ! RarArchiveFilePart(file)
                } else if (extension.equals("sfv")) {
                  logger.debug("sfv file detected: " + file)
                  archiveActor ! SfvFile(file)
                }
              }
            })

        }
      })
  })

  sys.addShutdownHook({
    logger.info("Shutdown called")
    system.terminate()
  })


}

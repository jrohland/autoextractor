package com.viciouswalrus.filesystem

import java.nio.file.{FileSystems, Path, WatchKey}
import java.nio.file.StandardWatchEventKinds._

import akka.actor.ActorRef
import org.apache.logging.log4j.scala.Logging

object WatchServiceTask {

  def apply(notifyActor: ActorRef) = new WatchServiceTask(notifyActor)

}

class WatchServiceTask(notifyActor: ActorRef) extends Runnable with Logging {
  private val watchService = FileSystems.getDefault.newWatchService()

  def run() {
    try {
      while (!Thread.currentThread().isInterrupted) {
        val key = watchService.take()

        key.pollEvents().forEach(event => {
          val relativePath = event.context().asInstanceOf[Path]
          val watchedDir = key.watchable().asInstanceOf[Path]
          val path = watchedDir.resolve(relativePath)
          event.kind() match {
            case ENTRY_CREATE =>
              if (path.toFile.isDirectory) {
                notifyActor ! DirectoryCreated(watchedDir, path)
              } else if (path.toFile.isFile) {
                notifyActor ! FileCreated(watchedDir, path)
              }
            case ENTRY_MODIFY =>
              if (path.toFile.isFile) {
                notifyActor ! FileUpdated(watchedDir, path)
              }
            case x =>
              logger.warn(s"Unknown event $x")
          }
        })

        key.reset()
      }
    } catch {
      case e: InterruptedException =>
        logger.debug("Interrupting, bye!")
    } finally {
      watchService.close()
    }
  }

  def watch(path: Path): WatchKey =
    path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY)

}

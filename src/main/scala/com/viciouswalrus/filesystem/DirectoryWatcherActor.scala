package com.viciouswalrus.filesystem

import java.nio.file.Path

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object DirectoryWatcherActor {
  def apply(system: ActorSystem, handler: FileSystemChangeHandler): ActorRef = {
    system.actorOf(Props(classOf[DirectoryWatcherActor], handler))
  }
}

class DirectoryWatcherActor(handler: FileSystemChangeHandler) extends Actor {

  val watchServiceTask = new WatchServiceTask(self)
  val watchThread = new Thread(watchServiceTask, "WatchService")

  def watch(path: Path): Unit = {
    watchServiceTask.watch(path)
  }

  override def preStart() {
    watchThread.setDaemon(true)
    watchThread.start()
  }

  override def postStop() {
    watchThread.interrupt()
  }

  override def receive: PartialFunction[Any, Unit] = {
    case WatchDirectory(watchDir) => watchServiceTask.watch(watchDir)
    case DirectoryCreated(watchedDir, createdDir) => handler.directoryCreated(watchedDir, createdDir)
    case FileCreated(_, file) => handler.fileCreated(file)
    case FileUpdated(_, file) => handler.fileUpdated(file)
  }

}

package com.viciouswalrus.autoextractor

import java.nio.file.Path

import akka.actor.ActorSystem
import com.viciouswalrus.filesystem.FileSystemChangeHandler
import org.apache.logging.log4j.scala.Logging

class FileCreatedHandler(system: ActorSystem, watchedDirectory: Path, outputPath: Path)
  extends FileSystemChangeHandler with Logging {

  val archiveActor = ArchiveActor(system, watchedDirectory, outputPath: Path)

  override def fileCreated(filePath: Path): Unit = {
    logger.debug("File created: " + filePath.toString)
    //fileAction(filePath)
  }

  override def fileUpdated(filePath: Path): Unit = {
    logger.debug("File updated: " + filePath.toString)
    fileAction(filePath)
  }

  private def fileAction(filePath: Path) = {
    val extension = filePath.toFile.toString.substring(filePath.toFile.toString.lastIndexOf('.') + 1).toLowerCase
    if (extension.startsWith("r")) {
      logger.debug("Rar file detected: " + filePath)
      archiveActor ! RarPart(filePath)
    } else if (extension.equals("sfv")) {
      logger.debug("sfv file detected: " + filePath)
      archiveActor ! Sfv(filePath)
    }
  }

}

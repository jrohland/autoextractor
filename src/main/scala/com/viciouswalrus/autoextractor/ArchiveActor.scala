package com.viciouswalrus.autoextractor

import java.nio.file.{Files, Path, Paths}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.github.junrar.extract.ExtractArchive
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable

object ArchiveActor {
  def apply(system: ActorSystem, inputPath: Path, outputPath: Path): ActorRef = {
    system.actorOf(Props(classOf[ArchiveActor], inputPath, outputPath))
  }
}

class ArchiveActor(inputPath: Path, outputPath: Path) extends Actor with Logging {

  private val rarFiles = mutable.MutableList.empty[Path]
  private var sfvFile: Sfv = _
  private var archiveComplete = false

  Files.list(inputPath).forEach(filePath => {
    val extension = filePath.toFile.toString.substring(filePath.toFile.toString.lastIndexOf('.') + 1).toLowerCase
    if (extension.startsWith("r")) {
      rarFiles.synchronized {
        if (!rarFiles.contains(filePath)) {
          logger.debug("Rar file detected: " + filePath)
          rarFiles += filePath
        }
      }
    } else if (extension.equals("sfv")) {
      this.synchronized {
        if (sfvFile == null) {
          logger.debug("sfv file detected: " + filePath)
          sfvFile = Sfv(filePath)
        }
      }

    }
  })
  checkIfArchiveIsComplete()

  override def receive: PartialFunction[Any, Unit] = {
    case RarPart(file) =>
      rarFiles.synchronized {
        if (!rarFiles.contains(file)) {
          logger.debug("received rar file")
          rarFiles += file
          checkIfArchiveIsComplete()
        }
      }
    case file: Sfv =>
      this.synchronized {
        if (sfvFile == null) {
          logger.debug("received sfv file")
          sfvFile = file
          checkIfArchiveIsComplete()
        }
      }
  }

  private def checkIfArchiveIsComplete(): Unit = {
    this.synchronized {
      if (!archiveComplete) {
        if (sfvFile != null && rarFiles.length >= sfvFile.fileContents.size) {
          val foundFiles = rarFiles.map(file => {
            file.getFileName.toString
          }).toSet

          val missingFiles = sfvFile.fileContents.keys.filter(fileName => {
            !foundFiles.contains(fileName)
          })

          if (missingFiles.nonEmpty) {
            logger.debug("Archive missing " + missingFiles.size + " files")
          } else {
            logger.debug("Archive complete")

            logger.debug("Extracting archive: " + inputPath.getFileName.toString)
            val rarFile = rarFiles.filter(file => {
              val extension = file.toString.substring(file.toString.lastIndexOf('.') + 1).toLowerCase
              extension == "rar"
            }).head

            val newOutputDir = Paths.get(outputPath.toString, inputPath.getFileName.toString)
            Files.createDirectory(newOutputDir)

            val extractor = new ExtractArchive()
            extractor.extractArchive(rarFile.toFile, newOutputDir.toFile)
            logger.info("Done extracting " + inputPath.getFileName.toString)

            archiveComplete = true

          }
        }
      }
    }
  }

}

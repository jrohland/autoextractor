package com.viciouswalrus

import java.nio.file.{Files, Path, Paths}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.github.junrar.extract.ExtractArchive
import com.roundeights.hasher.Algo
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable
import scala.concurrent.duration._

object ArchiveActor {
  def apply(inputPath: Path, outputPath: Path, directoryMonitor: ActorRef): Props = {
    Props(classOf[ArchiveActor], inputPath, outputPath, directoryMonitor)
  }
}

class ArchiveActor(inputPath: Path, outputPath: Path, directoryMonitor: ActorRef) extends Actor with Logging {

  implicit val system: ActorSystem = ActorSystem("actorSystem")
  import system.dispatcher

  private var directoryWatcherTerminated = false
  private val rarFiles = mutable.MutableList.empty[RarArchiveFilePart]
  private var sfvFile: SfvFile = _

  system.scheduler.scheduleOnce(30 seconds) {
    if (!directoryWatcherTerminated) {
      logger.warn("Unable to process directory: " + inputPath.toString)
      system.stop(directoryMonitor)
    }
  }

  override def receive: PartialFunction[Any, Unit] = {
    case file: RarArchiveFilePart =>
      logger.debug("received rar file")
      rarFiles += file
      checkIfArchiveIsComplete()
    case file: SfvFile =>
      logger.debug("received sfv file")
      sfvFile = file
      checkIfArchiveIsComplete()
  }

  private def checkIfArchiveIsComplete(): Unit = {
    if (sfvFile != null && rarFiles.length >= sfvFile.fileContents.size) {
      val foundFiles = rarFiles.map(file => {
        file.path.getFileName.toString
      }).toSet

      val missingFiles = sfvFile.fileContents.keys.filter(fileName => {
        !foundFiles.contains(fileName)
      })

      if (missingFiles.nonEmpty) {
        logger.debug("Archive missing " + missingFiles.size + " files")
      } else {
        logger.debug("Archive complete")
        system.stop(directoryMonitor)
        directoryWatcherTerminated = true

        val archiveValid = !Config.checkCRC || checkArchiveSFV()
        if (archiveValid) {
          logger.debug("Extracting archive: " + inputPath.getFileName.toString)
          val rarFile = rarFiles.filter(file => {
            val extension = file.path.toString.substring(file.path.toString.lastIndexOf('.') + 1).toLowerCase
            extension == "rar"
          }).head

          val newOutputDir = Paths.get(outputPath.toString, inputPath.getFileName.toString)
          Files.createDirectory(newOutputDir)

          val extractor = new ExtractArchive()
          extractor.extractArchive(rarFile.path.toFile, newOutputDir.toFile)
          logger.info("Done extracting " + inputPath.getFileName.toString)
        }
      }
    }
  }

  private def checkArchiveSFV(): Boolean = {
    val badFiles = rarFiles.filter(rarFile => {
      val filename = rarFile.path.getFileName.toString
      logger.debug("Checking crc32 for " + filename)
      val stream = Algo.crc32.tap(Files.newInputStream(rarFile.path))
      while ( stream.read() != -1 ) {}
      val hash = stream.hash
      logger.debug("crc32: " + hash)
      hash.toString() != sfvFile.fileContents(filename)
    })

    if (badFiles.nonEmpty) {
      logger.warn("Bad files found in archive " + inputPath.getFileName.toString)
      badFiles.foreach(file => {
        logger.warn("Bad file: " + file.path.getFileName.toString)
      })
    } else {
      logger.debug("All files are good")
    }

    badFiles.nonEmpty
  }

}

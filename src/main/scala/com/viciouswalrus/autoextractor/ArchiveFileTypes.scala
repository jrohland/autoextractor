package com.viciouswalrus.autoextractor

import java.nio.file.{FileSystemException, Files, Path}

import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConverters._

sealed trait ArchiveFileTypes

case class RarPart(path: Path) extends ArchiveFileTypes
case class Sfv(path: Path) extends ArchiveFileTypes with Logging {
  private var contents = Map.empty[String, String]

  def fileContents: Map[String, String] = {
    this.synchronized {
      while (contents.isEmpty) {
        try {
          contents = Files.readAllLines(path).asScala.filter(!_.isEmpty).map(sfv => {
            val contents = sfv.split(" ")
            contents(0) -> contents(1)
          }).toMap
        } catch {
          case e: FileSystemException =>
            logger.warn("Couldn't read sfv file, sleeping to try again", e)
            Thread.sleep(500)
        }
      }
    }

    contents
  }

}

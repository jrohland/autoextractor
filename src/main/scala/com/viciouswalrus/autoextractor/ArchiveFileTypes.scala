package com.viciouswalrus.autoextractor

import java.nio.file.{Files, Path}

import scala.collection.JavaConverters._

sealed trait ArchiveFileTypes

case class RarPart(path: Path) extends ArchiveFileTypes
case class Sfv(path: Path) extends ArchiveFileTypes {
  private var contents = Map.empty[String, String]

  def fileContents: Map[String, String] = {
    if (contents.isEmpty) {
      contents = Files.readAllLines(path).asScala.filter(!_.isEmpty).map(sfv => {
        val contents = sfv.split(" ")
        contents(0) -> contents(1)
      }).toMap
    }
    contents
  }

}

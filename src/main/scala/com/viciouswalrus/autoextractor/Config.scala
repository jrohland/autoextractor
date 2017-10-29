package com.viciouswalrus.autoextractor

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

object Config {

  private val config = ConfigFactory.load()

  val watchedDirs: List[WatchedDirectory] = config.getObjectList("watched-directories").asScala.map(dir => {
    WatchedDirectory(
      input = Paths.get(dir.get("input").unwrapped().toString),
      output = Paths.get(dir.get("output").unwrapped().toString)
    )
  }).toList

  val checkCRC: Boolean = config.getBoolean("check-crc")

}

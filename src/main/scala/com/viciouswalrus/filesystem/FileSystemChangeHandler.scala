package com.viciouswalrus.filesystem

import java.nio.file.Path

trait FileSystemChangeHandler {

  def directoryCreated(watchedDir: Path, createdDir: Path) {}
  def fileCreated(path: Path) {}
  def fileUpdated(path: Path) {}

}

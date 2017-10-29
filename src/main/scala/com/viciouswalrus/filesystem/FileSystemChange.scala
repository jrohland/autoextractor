package com.viciouswalrus.filesystem

import java.nio.file.Path

sealed trait FileSystemChange

case class WatchDirectory(watchDir: Path) extends FileSystemChange

case class DirectoryCreated(watchedDir: Path, dir: Path) extends FileSystemChange
case class FileCreated(watchedDir: Path, filePath: Path) extends FileSystemChange
case class FileUpdated(watchedDir: Path, filePath: Path) extends FileSystemChange

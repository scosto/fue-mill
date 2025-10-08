package com.example

import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.report
import java.nio.file.{Files, Path, Paths}
import java.io.PrintWriter
import scala.collection.mutable.ListBuffer

class MyCompilerPlugin extends StandardPlugin {
  val name: String = "myplugin"
  override val description: String = "Example Scala 3 Compiler Plugin"

  override def initialize(options: List[String])(using Context): List[PluginPhase] = {
    report.echo("MyCompilerPlugin loaded!")
    List(
      new MyPluginPhase(options),
      new TastyAnalyzerPhase()
    )
  }
}

class MyPluginPhase(options: List[String]) extends PluginPhase {
  import tpd._

  val phaseName = "myplugin"

  override val runsAfter = Set("typer")
  override val runsBefore = Set("pickler")

  private val verbose = options.exists(_.startsWith("verbose:true"))
  private var warningCount = 0

  override def transformDefDef(tree: DefDef)(using Context): DefDef = {
    if (verbose) {
      report.echo(s"Processing method: ${tree.name}")
    }
    tree
  }

  override def transformValDef(tree: ValDef)(using Context): ValDef = {
    if (verbose) {
      report.echo(s"Processing value: ${tree.name}")
    }

    // Warnung für Variablen die mit 'c' beginnen
    val varName = tree.name.toString
    if (varName.startsWith("c")) {
      val msg = s"Variable '$varName' starts with 'c' - consider using a more descriptive name"
      report.warning(msg, tree.srcPos)
      warningCount += 1
      PluginState.warningCount += 1
      PluginState.addValidationLog("warning", s"${tree.sourcePos}: $msg")
    }

    tree
  }

  override def transformUnit(tree: tpd.Tree)(using Context): tpd.Tree = {
    val result = super.transformUnit(tree)

    // Write validation status nach Unit-Verarbeitung
    // Finde das Projekt-Root-Verzeichnis durch Source-File-Path
    val sourcePath = tree.sourcePos.source.file.path
    val workingDir = findProjectRoot(sourcePath)
    if (PluginState.validationLogs.isEmpty) {
      PluginState.addValidationLog("success", "No validation warnings found")
    }
    StatusWriter.writeValidationStatus(workingDir, PluginState.validationLogs.toSeq)

    result
  }

  private def findProjectRoot(sourcePath: String): String = {
    // Gehe vom Source-File nach oben bis wir build.sc oder .git finden
    val sourceFile = new java.io.File(sourcePath)
    var dir = sourceFile.getParentFile
    while (dir != null) {
      if (new java.io.File(dir, "build.sc").exists() ||
          new java.io.File(dir, ".git").exists()) {
        return dir.getAbsolutePath
      }
      dir = dir.getParentFile
    }
    // Fallback auf user.dir
    sys.props.get("user.dir").getOrElse(".")
  }
}

// Shared state für Warning-Count
object PluginState {
  var warningCount: Int = 0
  val validationLogs = ListBuffer.empty[(String, String)]
  val tastyLogs = ListBuffer.empty[(String, String)]

  def resetWarnings(): Unit = {
    warningCount = 0
    validationLogs.clear()
    tastyLogs.clear()
  }

  def addValidationLog(level: String, message: String): Unit = {
    validationLogs += ((level, message))
  }

  def addTastyLog(level: String, message: String): Unit = {
    tastyLogs += ((level, message))
  }
}

// Status file writer
object StatusWriter {
  def getStateDir(workingDir: String): Path = {
    val stateDir = Paths.get(workingDir, ".mill-plugin-state")
    if (!Files.exists(stateDir)) {
      Files.createDirectories(stateDir)
    }
    stateDir
  }

  def writeValidationStatus(workingDir: String, logs: Seq[(String, String)]): Unit = {
    try {
      val stateDir = getStateDir(workingDir)
      val statusFile = stateDir.resolve("validation.json")

      val logsJson = logs.map { case (level, msg) =>
        s"""{"level":"$level","message":"${escapeJson(msg)}"}"""
      }.mkString(",")

      val json = s"""{
        "timestamp": "${java.time.Instant.now()}",
        "logs": [$logsJson]
      }"""

      val writer = new PrintWriter(statusFile.toFile)
      try {
        writer.write(json)
      } finally {
        writer.close()
      }
    } catch {
      case e: Exception =>
        System.err.println(s"Error writing validation status: ${e.getMessage}")
    }
  }

  def writeTastyStatus(workingDir: String, logs: Seq[(String, String)]): Unit = {
    try {
      val stateDir = getStateDir(workingDir)
      val statusFile = stateDir.resolve("tasty.json")

      val logsJson = logs.map { case (level, msg) =>
        s"""{"level":"$level","message":"${escapeJson(msg)}"}"""
      }.mkString(",")

      val json = s"""{
        "timestamp": "${java.time.Instant.now()}",
        "logs": [$logsJson]
      }"""

      val writer = new PrintWriter(statusFile.toFile)
      try {
        writer.write(json)
      } finally {
        writer.close()
      }
    } catch {
      case e: Exception =>
        System.err.println(s"Error writing tasty status: ${e.getMessage}")
    }
  }

  private def escapeJson(s: String): String = {
    s.replace("\\", "\\\\")
     .replace("\"", "\\\"")
     .replace("\n", "\\n")
     .replace("\r", "\\r")
     .replace("\t", "\\t")
  }
}

class TastyAnalyzerPhase extends PluginPhase {
  import tpd._

  val phaseName = "tastyanalyzer"

  override val runsAfter = Set("genBCode")

  override def transformUnit(tree: Tree)(using Context): Tree = {
    // Finde das Projekt-Root-Verzeichnis
    val workingDir = findProjectRoot(tree.sourcePos.source.file.path)

    // Nur analysieren wenn keine Warnungen
    if (PluginState.warningCount == 0) {
      PluginState.addTastyLog("success", "Starting TASTy analysis...")
      analyzeTastyFiles()
    } else {
      val msg = s"Skipping TASTy analysis due to ${PluginState.warningCount} warning(s)"
      report.echo(msg)
      PluginState.addTastyLog("error", msg)
    }

    // Write TASTy status
    StatusWriter.writeTastyStatus(workingDir, PluginState.tastyLogs.toSeq)

    // Reset für nächste Kompilierung
    PluginState.resetWarnings()
    tree
  }

  private def findProjectRoot(sourcePath: String): String = {
    // Gehe vom Source-File nach oben bis wir build.sc oder .git finden
    val sourceFile = new java.io.File(sourcePath)
    var dir = sourceFile.getParentFile
    while (dir != null) {
      if (new java.io.File(dir, "build.sc").exists() ||
          new java.io.File(dir, ".git").exists()) {
        return dir.getAbsolutePath
      }
      dir = dir.getParentFile
    }
    // Fallback auf user.dir
    sys.props.get("user.dir").getOrElse(".")
  }

  private def analyzeTastyFiles()(using ctx: Context): Unit = {
    val outputDir = ctx.settings.outputDir.value
    val msg = s"Analyzing TASTy files in: $outputDir"
    report.echo(msg)
    PluginState.addTastyLog("success", msg)

    // TASTy-Dateien finden und analysieren
    import scala.jdk.CollectionConverters._

    try {
      val tastyFiles = Files.walk(Paths.get(outputDir.toString))
        .iterator()
        .asScala
        .filter(p => p.toString.endsWith(".tasty"))
        .toList

      if (tastyFiles.isEmpty) {
        val noFilesMsg = "No TASTy files found yet"
        report.echo(noFilesMsg)
        PluginState.addTastyLog("warning", noFilesMsg)
      } else {
        val foundMsg = s"Found ${tastyFiles.size} TASTy file(s)"
        report.echo(foundMsg)
        PluginState.addTastyLog("success", foundMsg)

        tastyFiles.foreach { path =>
          val size = Files.size(path)
          val fileMsg = s"  ${path.getFileName} - $size bytes"
          report.echo(fileMsg)
          PluginState.addTastyLog("success", fileMsg)
        }
      }
    } catch {
      case e: Exception =>
        val errMsg = s"Error analyzing TASTy files: ${e.getMessage}"
        report.echo(errMsg)
        PluginState.addTastyLog("error", errMsg)
    }
  }
}

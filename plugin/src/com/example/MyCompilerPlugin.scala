package com.example

import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.report

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
      report.warning(s"Variable '$varName' starts with 'c' - consider using a more descriptive name", tree.srcPos)
      warningCount += 1
      PluginState.warningCount += 1
    }

    tree
  }
}

// Shared state für Warning-Count
object PluginState {
  var warningCount: Int = 0
  def resetWarnings(): Unit = warningCount = 0
}

class TastyAnalyzerPhase extends PluginPhase {
  import tpd._

  val phaseName = "tastyanalyzer"

  override val runsAfter = Set("genBCode")

  override def transformUnit(tree: Tree)(using Context): Tree = {
    // Nur analysieren wenn keine Warnungen
    if (PluginState.warningCount == 0) {
      analyzeTastyFiles()
    } else {
      report.echo(s"Skipping TASTy analysis due to ${PluginState.warningCount} warning(s)")
    }

    // Reset für nächste Kompilierung
    PluginState.resetWarnings()
    tree
  }

  private def analyzeTastyFiles()(using ctx: Context): Unit = {
    val outputDir = ctx.settings.outputDir.value
    report.echo(s"Analyzing TASTy files in: $outputDir")

    // TASTy-Dateien finden und analysieren
    import java.nio.file.{Files, Path, Paths}
    import scala.jdk.CollectionConverters._

    try {
      val tastyFiles = Files.walk(Paths.get(outputDir.toString))
        .iterator()
        .asScala
        .filter(p => p.toString.endsWith(".tasty"))
        .toList

      if (tastyFiles.isEmpty) {
        report.echo("No TASTy files found yet")
      } else {
        report.echo(s"Found ${tastyFiles.size} TASTy file(s):")
        tastyFiles.foreach { path =>
          val size = Files.size(path)
          report.echo(s"  ${path.getFileName} - $size bytes")
        }
      }
    } catch {
      case e: Exception =>
        report.echo(s"Error analyzing TASTy files: ${e.getMessage}")
    }
  }
}

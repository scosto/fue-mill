package com.example

import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.report

class MyCompilerPlugin extends StandardPlugin {
  val name: String = "myplugin"
  override val description: String = "Example Scala 3 Compiler Plugin"

  override def init(options: List[String])(using Context): List[PluginPhase] = {
    List(new MyPluginPhase(options))
  }
}

class MyPluginPhase(options: List[String]) extends PluginPhase {
  import tpd._

  val phaseName = "myplugin"

  override val runsAfter = Set("typer")
  override val runsBefore = Set("pickler")

  private val verbose = options.exists(_.startsWith("verbose:true"))

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
    tree
  }
}

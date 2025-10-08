package com.example

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.file.{Files, Paths, Path}
import java.io.{ByteArrayOutputStream, PrintStream}
import scala.jdk.CollectionConverters._
import dotty.tools.dotc.Main

class PluginTest extends AnyFunSuite with Matchers {

  test("Plugin loads and compiles code successfully") {
    val sourceCode = """
      |package test
      |object Test {
      |  def foo(): Int = 42
      |}
    """.stripMargin

    val result = compileWithPlugin(sourceCode, "SimpleTest")

    result.exitCode.shouldBe(0)
    result.tastyFiles.size should be > 0
  }

  test("Plugin creates TASTy files") {
    val sourceCode = """
      |package test
      |object TestTasty {
      |  def bar(): String = "hello"
      |}
    """.stripMargin

    val result = compileWithPlugin(sourceCode, "TastyTest")

    result.exitCode.shouldBe(0)
    result.tastyFiles.size should be > 0
    result.tastyFiles.exists(_.endsWith(".tasty")).shouldBe(true)
  }

  test("Plugin processes multiple files") {
    val sourceCode = """
      |package test
      |class Processor {
      |  def process(x: Int): Int = x * 2
      |}
      |object ProcessorMain {
      |  val p = new Processor
      |  def main(args: Array[String]): Unit = {
      |    println(p.process(21))
      |  }
      |}
    """.stripMargin

    val result = compileWithPlugin(sourceCode, "MultiTest")

    result.exitCode.shouldBe(0)
    result.tastyFiles.size should be >= 2 // Class + Object
  }

  // Helper-Methode zum Kompilieren mit Plugin
  private def compileWithPlugin(
      sourceCode: String,
      testName: String
  ): CompilationResult = {
    val sourceDir = Files.createTempDirectory(s"plugin-test-src-$testName")
    val outputDir = Files.createTempDirectory(s"plugin-test-out-$testName")

    val pluginJar = {
      val paths = List(
        Paths.get("out/plugin/jar.dest/out.jar"),
        Paths.get("../../../out/plugin/jar.dest/out.jar"),
        Paths.get(sys.props("user.dir"), "out/plugin/jar.dest/out.jar")
      )

      paths
        .find(Files.exists(_))
        .map(_.toAbsolutePath.toString)
        .getOrElse {
          fail(s"Plugin JAR not found. Run: ./mill plugin.jar")
        }
    }

    try {
      val sourceFile = sourceDir.resolve("Test.scala")
      Files.writeString(sourceFile, sourceCode)

      val outputStream = new ByteArrayOutputStream()
      val printStream = new PrintStream(outputStream)
      val oldOut = System.out
      val oldErr = System.err

      try {
        System.setOut(printStream)
        System.setErr(printStream)

        val args = Array(
          "-classpath",
          sys.props("java.class.path"),
          s"-Xplugin:$pluginJar",
          "-d",
          outputDir.toString,
          sourceFile.toString
        )

        val exitCode =
          try {
            Main.process(args)
            0
          } catch {
            case e: Exception =>
              println(s"Compilation failed: ${e.getMessage}")
              1
          }

        printStream.flush()
        val output = outputStream.toString("UTF-8")

        val tastyFiles = if (Files.exists(outputDir)) {
          Files
            .walk(outputDir)
            .iterator()
            .asScala
            .filter(p => p.toString.endsWith(".tasty"))
            .map(_.toString)
            .toList
        } else {
          List.empty
        }

        CompilationResult(
          output = output,
          exitCode = exitCode,
          tastyFiles = tastyFiles
        )
      } finally {
        System.setOut(oldOut)
        System.setErr(oldErr)
      }
    } finally {
      deleteRecursively(sourceDir)
      deleteRecursively(outputDir)
    }
  }

  private def deleteRecursively(path: Path): Unit = {
    if (Files.exists(path)) {
      Files
        .walk(path)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(Files.delete)
    }
  }

  case class CompilationResult(
      output: String,
      exitCode: Int,
      tastyFiles: List[String]
  )
}

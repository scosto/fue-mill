import mill._
import mill.scalalib._

object plugin extends ScalaModule {
  def scalaVersion = "3.7.3"

  def ivyDeps = Agg(
    ivy"org.scala-lang::scala3-compiler:${scalaVersion()}"
  )

  // Plugin Properties automatisch generieren
  def resources = T.sources {
    os.write(
      T.dest / "plugin.properties",
      s"pluginClass=com.example.MyCompilerPlugin"
    )
    super.resources() ++ Seq(PathRef(T.dest))
  }
}

object example extends ScalaModule {
  def scalaVersion = "3.7.3"

  // Plugin direkt verwenden
  def scalacPluginClasspath = T{ Seq(plugin.jar()) }

  // Plugin-Optionen
  def scalacOptions = Seq(
    "-P:myplugin:verbose:true",
    "-Xprint:myplugin"  // Zeigt Plugin-Transformationen
  )

  def moduleDeps = Seq(plugin)
}

// Test-Modul für Plugin-Tests
object tests extends ScalaModule {
  def scalaVersion = "3.7.3"

  def ivyDeps = Agg(
    ivy"org.scalatest::scalatest:3.2.19"
  )

  def scalacPluginClasspath = T{ Seq(plugin.jar()) }

  def testFramework = "org.scalatest.tools.Framework"
}

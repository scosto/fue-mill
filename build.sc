import mill._, mill.scalalib._

object plugin extends ScalaModule {
  def scalaVersion = "3.7.3-RC3"

  def mvnDeps = Seq(
    mvn"org.scala-lang::scala3-compiler:3.7.3-RC3"
  )

  // Plugin Properties automatisch generieren
  override def resources = Task {
    os.write(
      Task.dest / "plugin.properties",
      s"pluginClass=com.example.MyCompilerPlugin"
    )
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}

object example extends ScalaModule {
  def scalaVersion = "3.7.3-RC3"

  // Plugin direkt verwenden
  override def scalacPluginClasspath = Task { Seq(plugin.jar()) }

  // Plugin-Optionen
  override def scalacOptions = Task {
    super.scalacOptions() ++ Seq(
      s"-Xplugin:${plugin.jar().path}"
    )
  }

  def moduleDeps = Seq(plugin)

  object test extends ScalaTests {
    def mvnDeps = Seq(
      mvn"org.scalatest::scalatest:3.2.19"
    )

    def testFramework = "org.scalatest.tools.Framework"
  }
}

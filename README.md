# Scala 3 Compiler Plugin Development mit DevPod

Ein vollständiges Setup für Scala 3 Compiler-Plugin-Entwicklung mit Mill, VSCode und DevPod.

## 🚀 Quick Start

### Voraussetzungen

- [DevPod](https://devpod.sh/) installiert
- VSCode (optional, aber empfohlen)

### Setup mit DevPod

```bash
# 1. DevPod installieren (falls noch nicht geschehen)
# Windows (mit winget):
winget install devpod

# Linux:
curl -L -o devpod "https://github.com/loft-sh/devpod/releases/latest/download/devpod-linux-amd64"
chmod +x devpod
sudo mv devpod /usr/local/bin

# macOS:
brew install devpod

# 2. Workspace erstellen und öffnen
devpod up . --ide vscode

# VSCode öffnet sich automatisch mit vollem Metals-Support!
```

### Manuelles Setup (ohne DevPod)

Falls du den Container manuell starten möchtest:

```bash
# Container bauen und starten
cd .devcontainer
docker build -t scala-plugin-dev .
docker run -it -v $(pwd)/..:/workspace scala-plugin-dev

# Im Container
mill __.compile
```

## 📁 Projektstruktur

```
.
├── .devcontainer/
│   ├── Dockerfile              # Container-Definition
│   └── devcontainer.json       # VSCode/DevPod-Konfiguration
├── .vscode/
│   ├── launch.json             # Debug-Konfiguration
│   └── tasks.json              # Build-Tasks
├── plugin/
│   └── src/com/example/
│       └── MyCompilerPlugin.scala  # Compiler-Plugin
├── example/
│   └── src/com/example/
│       └── TestMain.scala      # Test-Beispiel
├── tests/
│   └── src/com/example/
│       └── PluginTest.scala    # Plugin-Tests
└── build.sc                    # Mill Build-Definition
```

## 🛠️ Entwicklung

### Mit Mill (empfohlen)

```bash
# Alles kompilieren
mill __.compile

# Continuous Compilation (Watch Mode)
mill -w __.compile

# Tests ausführen
mill tests.test

# Beispiel mit Plugin ausführen
mill example.run

# Plugin-JAR bauen
mill plugin.jar
```

### Mit VSCode Tasks

- **Ctrl+Shift+B**: Continuous Compile starten
- **Ctrl+Shift+P** → "Run Test Task": Tests ausführen

### Debugging

1. Setze Breakpoints in VSCode
2. Drücke **F5** oder nutze "Debug Plugin Test"
3. Der Debugger verbindet sich automatisch

## 🔧 Plugin-Optionen

Das Plugin kann über `scalacOptions` in `build.sc` konfiguriert werden:

```scala
def scalacOptions = Seq(
  "-P:myplugin:verbose:true",    // Verbose-Modus aktivieren
  "-Xprint:myplugin"              // Plugin-Transformationen anzeigen
)
```

## 📝 Plugin-Entwicklung

### Plugin-Struktur

```scala
class MyCompilerPlugin extends StandardPlugin {
  val name: String = "myplugin"
  override val description: String = "Example Compiler Plugin"

  def init(options: List[String]): List[PluginPhase] = {
    List(new MyPluginPhase(options))
  }
}

class MyPluginPhase(options: List[String]) extends PluginPhase {
  val phaseName = "myplugin"

  override val runsAfter = Set("typer")
  override val runsBefore = Set("pickler")

  // Transformationen hier implementieren
  override def transformDefDef(tree: DefDef)(using Context): DefDef = {
    // Method-Definitionen transformieren
    tree
  }
}
```

### Plugin-Optionen verarbeiten

```scala
private val verbose = options.exists(_.startsWith("verbose:true"))
private val config = options.collectFirst {
  case s"config:$path" => path
}
```

## 🎯 DevPod Features

### Lokale Entwicklung
```bash
devpod up . --ide vscode
```

### Remote-Entwicklung (SSH)
```bash
devpod provider add ssh
devpod up . --provider ssh --ide vscode
```

### Cloud-Entwicklung (AWS/Azure/GCP)
```bash
devpod provider add aws
devpod up . --provider aws --ide vscode
```

## 🚀 Performance-Tipps

1. **Volume Mounts**: Bereits in `devcontainer.json` konfiguriert für schnellere Builds
2. **Mill Watch Mode**: Nutze `-w` für kontinuierliche Kompilierung
3. **BSP Support**: Metals nutzt automatisch Mill's Build Server Protocol

## 📚 Nützliche Befehle

```bash
# Mill-Version anzeigen
mill version

# Alle verfügbaren Targets zeigen
mill resolve _

# Spezifisches Modul kompilieren
mill plugin.compile
mill example.compile

# Clean Build
mill clean
mill __.compile

# Dependency Tree anzeigen
mill show plugin.ivyDeps

# REPL mit Plugin-Classpath
mill plugin.repl
```

## 🐛 Troubleshooting

### Metals lädt nicht

```bash
# Im Container:
mill mill.bsp.BSP/install
# Dann in VSCode: Cmd/Ctrl+Shift+P → "Metals: Restart Server"
```

### Plugin wird nicht erkannt

Prüfe `plugin/resources/plugin.properties`:
```properties
pluginClass=com.example.MyCompilerPlugin
```

### DevPod-Container startet nicht

```bash
# Logs anzeigen
devpod logs .

# Container neu bauen
devpod delete .
devpod up . --ide vscode --recreate
```

## 📖 Weitere Ressourcen

- [Scala 3 Compiler Plugin Guide](https://docs.scala-lang.org/scala3/guides/migration/plugin-intro.html)
- [Mill Documentation](https://mill-build.com/)
- [DevPod Documentation](https://devpod.sh/docs)
- [Metals LSP](https://scalameta.org/metals/)

## 🤝 Beitragen

Dieses Setup kann als Template für eigene Compiler-Plugin-Projekte verwendet werden.

## 📄 Lizenz

Dieses Projekt dient als Beispiel-Setup und kann frei verwendet werden.

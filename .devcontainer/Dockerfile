# Nutze fertiges Scala-Image mit Java
FROM eclipse-temurin:21-jdk

# Installiere grundlegende Tools
RUN apt-get update && apt-get install -y \
    curl \
    git \
    bash \
    sudo \
    && rm -rf /var/lib/apt/lists/*

# Erstelle vscode User (für devcontainer)
RUN useradd -ms /bin/bash vscode \
    && echo "vscode ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Install Coursier (für Scala-Tools)
RUN curl -fL "https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz" | gzip -d > /usr/local/bin/cs \
    && chmod +x /usr/local/bin/cs

# Install Mill 1.0.6 direkt
RUN curl -L https://github.com/com-lihaoyi/mill/releases/download/1.0.6/1.0.6-assembly > /usr/local/bin/mill \
    && chmod +x /usr/local/bin/mill

# Als vscode User: Setup Scala tooling mit Defaults
USER vscode
RUN cs setup --yes

# PATH für Coursier binaries
ENV PATH="/home/vscode/.local/share/coursier/bin:$PATH"

# Workspace vorbereiten
WORKDIR /workspace

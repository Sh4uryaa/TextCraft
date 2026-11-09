#!/usr/bin/env bash
# Builds a native installer/app-image with a small runtime made by the SAME JDK (needs JDK 14+ for jpackage).
# Usage: scripts/package.sh        (run from the project root, after: mvn package)
set -euo pipefail
JAR=target/textcraft-1.0.0.jar
[ -f "$JAR" ] || { echo "Run 'mvn package' first"; exit 1; }

# 1. work out which JDK modules the app needs, add the ones that are loaded dynamically
MODS=$(jdeps --ignore-missing-deps --multi-release 11 --print-module-deps "$JAR")
MODS="$MODS,java.sql,java.naming,java.net.http,jdk.httpserver,jdk.crypto.ec,java.xml.crypto,java.prefs,jdk.unsupported"

# 2. small runtime image (jlink)
rm -rf build/runtime build/installer
jlink --add-modules "$MODS" --strip-debug --no-header-files --no-man-pages --compress=2 --output build/runtime

# 3. package (type defaults to the OS: msi/exe on Windows, dmg on macOS, deb/rpm on Linux; use --type app-image to just test)
mkdir -p build/input && cp "$JAR" build/input/ && mkdir -p build/input/config && cp config/app.properties.example build/input/config/
jpackage --name TextCraft --app-version 1.0.0 \
  --input build/input --main-jar "$(basename "$JAR")" --main-class com.textcraft.app.Main \
  --runtime-image build/runtime --java-options "-Xmx512m" --dest build/installer
echo "Installer written to build/installer"

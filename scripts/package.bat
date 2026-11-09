@echo off
rem Windows version of package.sh (needs JDK 14+ and, for .msi/.exe, the WiX toolset). Run after: mvn package
set JAR=target\textcraft-1.0.0.jar
if not exist %JAR% (echo Run mvn package first & exit /b 1)
for /f %%M in ('jdeps --ignore-missing-deps --multi-release 11 --print-module-deps %JAR%') do set MODS=%%M
set MODS=%MODS%,java.sql,java.naming,java.net.http,jdk.httpserver,jdk.crypto.ec,java.xml.crypto,java.prefs,jdk.unsupported
rmdir /s /q build 2>nul
jlink --add-modules %MODS% --strip-debug --no-header-files --no-man-pages --compress=2 --output build\runtime
mkdir build\input & copy %JAR% build\input\ >nul
mkdir build\input\config & copy config\app.properties.example build\input\config\ >nul
jpackage --name TextCraft --app-version 1.0.0 --input build\input --main-jar textcraft-1.0.0.jar --main-class com.textcraft.app.Main --runtime-image build\runtime --java-options "-Xmx512m" --dest build\installer

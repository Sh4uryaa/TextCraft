@echo off
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Users\krish\tools\jdk\jdk-21.0.13+9"
)
set "PATH=%JAVA_HOME%\bin;C:\Users\krish\tools\mvn\apache-maven-3.9.9\bin;%PATH%"

echo Launching NovaEdit (Java 21 + JavaFX 21)...
call ".\mvnw.cmd" clean javafx:run

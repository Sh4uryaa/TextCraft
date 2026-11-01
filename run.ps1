# NovaEdit PowerShell Launcher Script
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $env:JAVA_HOME = "C:\Users\krish\tools\jdk\jdk-21.0.13+9"
}
$mvnBin = "C:\Users\krish\tools\mvn\apache-maven-3.9.9\bin"
if ($env:PATH -notlike "*$env:JAVA_HOME\bin*") {
    $env:PATH = "$env:JAVA_HOME\bin;$mvnBin;$env:PATH"
}

Write-Host "Launching NovaEdit (Java 21 + JavaFX 21)..." -ForegroundColor Cyan
& ".\mvnw.cmd" clean javafx:run

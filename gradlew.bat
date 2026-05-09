@rem Gradle wrapper script for Windows CMD / PowerShell
@if "%DEBUG%"=="" @echo off
setlocal
set DIRNAME=%~dp0
set WRAPPER_JAR=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo ERROR: gradle-wrapper.jar not found at %WRAPPER_JAR%
    exit /b 1
)

java -jar "%WRAPPER_JAR%" %*

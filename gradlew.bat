@rem Gradle wrapper script for Windows CMD / PowerShell
@if "%DEBUG%"=="" @echo off
setlocal
set "DIRNAME=%~dp0"
set "WRAPPER_JAR=%DIRNAME%gradle\wrapper\gradle-wrapper.jar"

if exist "%WRAPPER_JAR%" goto run
echo ERROR: gradle-wrapper.jar not found at %WRAPPER_JAR%
exit /b 1

:run
java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*

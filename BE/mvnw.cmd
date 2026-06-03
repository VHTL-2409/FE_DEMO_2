@echo off
setlocal enabledelayedexpansion

set "JAVA_HOME=%JAVA_HOME%"
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
if "%JAVA_HOME%"=="" if exist "C:\Program Files\jdk-17.0.13+11\bin\java.exe" set "JAVA_HOME=C:\Program Files\jdk-17.0.13+11"
if "%JAVA_HOME%"=="" (
    echo JAVA_HOME is not set and no bundled JDK 17 path was found.
    echo Please set JAVA_HOME to a JDK 17 installation.
    exit /b 1
)

where mvn >nul 2>nul
if not errorlevel 1 (
    call mvn %*
    exit /b %ERRORLEVEL%
)

set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven wrapper...
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
)

"%JAVA_HOME%\bin\java.exe" "-Dmaven.multiModuleProjectDirectory=%~dp0" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*

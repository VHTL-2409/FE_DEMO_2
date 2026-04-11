@echo off
setlocal enabledelayedexpansion

set "JAVA_HOME=%JAVA_HOME%"
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\jdk-17.0.13+11"

set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven wrapper...
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
)

"%JAVA_HOME%\bin\java.exe" -Dmaven.multiModuleProjectDirectory="%~dp0" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*

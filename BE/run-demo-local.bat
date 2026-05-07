@echo off
if "%APP_AI_SERVICE_ENABLED%"=="" set APP_AI_SERVICE_ENABLED=true
if "%APP_AI_SERVICE_BASE_URL%"=="" set APP_AI_SERVICE_BASE_URL=http://localhost:8090
set APP_FRONTEND_BASE_URL=http://localhost:4173
set APP_URL=http://localhost:8082
set SPRING_PROFILES_ACTIVE=gmail
"C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -jar "%~dp0target\demo-0.0.1-SNAPSHOT.jar"

@echo off
REM Chạy backend với Gmail SMTP
REM Trước khi chạy, đặt biến môi trường:
REM   set SPRING_MAIL_USERNAME=your-email@gmail.com
REM   set SPRING_MAIL_PASSWORD=your-app-password
REM App password: https://myaccount.google.com/apppasswords

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=gmail"

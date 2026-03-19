FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
# Múi giờ Hồ Chí Minh (UTC+7)
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -sf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-Duser.timezone=Asia/Ho_Chi_Minh","-jar","/app/app.jar"]


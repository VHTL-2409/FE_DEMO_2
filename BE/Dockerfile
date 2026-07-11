FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
# dùng múi giờ việt nam
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# cài curl để docker healthcheck gọi được api
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
# chạy app bằng java, giữ timezone cho đúng giờ làm bài
ENTRYPOINT ["sh","-c","java $JAVA_TOOL_OPTIONS -Duser.timezone=Asia/Ho_Chi_Minh -jar /app/app.jar"]
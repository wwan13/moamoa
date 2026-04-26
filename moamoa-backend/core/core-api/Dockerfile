FROM eclipse-temurin:24-jre-alpine

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app.jar

EXPOSE 8081

ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","/app.jar"]
FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8081

ENTRYPOINT ["java","-jar","-Duser.timezone=Asia/Seoul","/app.jar"]

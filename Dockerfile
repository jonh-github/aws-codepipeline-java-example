FROM amazoncorretto:11-alpine-jdk
EXPOSE 8080
ARG JAR_FILE=target/spring-rest-example-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

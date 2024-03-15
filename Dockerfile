FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /build/target/SaveSyncServer-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
EXPOSE 1234
EXPOSE 443
ENTRYPOINT ["java", "-jar", "app.jar"]
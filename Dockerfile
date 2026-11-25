FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /project
COPY pom.xml .
COPY src src
RUN mvn -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /project/target/order-processing-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

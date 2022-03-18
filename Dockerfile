FROM maven:3.6.3-openjdk-17-slim as maven_builder
WORKDIR /app
ADD . .
RUN mvn package -Dmaven.test.skip=true

FROM openjdk:17.0-jdk-slim
COPY --from=maven_builder /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
FROM maven:3.6.3-openjdk-17-slim as maven_builder
WORKDIR /app
ADD . .
RUN mvn package -Dmaven.test.skip=true

FROM arm64v8/openjdk:11-oracle
COPY --from=maven_builder /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
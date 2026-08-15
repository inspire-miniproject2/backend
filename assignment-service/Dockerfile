FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


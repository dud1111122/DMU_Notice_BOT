FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=15.0 -XX:MaxRAMPercentage=50.0 -XX:+UseStringDeduplication -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]

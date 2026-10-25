# Build: Maven (via the wrapper) on a full JDK 21.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
# Download dependencies in their own layer so code changes rebuild quickly.
RUN ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -DskipTests package

# Run: slim JRE 21 as a non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
COPY --from=build /workspace/target/footballhub-api.jar app.jar
USER app
# Render sets PORT; application.yml reads it (default 8080).
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

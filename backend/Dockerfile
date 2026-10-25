# Build: Maven (via the wrapper) on a full JDK 21.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
# Download dependencies in their own layer so code changes rebuild quickly.
RUN ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/footballhub-api.jar extract --destination /workspace/extracted

# Run: slim JRE 21 as a non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
COPY --from=build /workspace/extracted/ ./

# Tuned for small instances (Render free: 0.1 CPU, 512 MB). C1-only JIT and the serial GC
# spend far less CPU while starting up than the defaults, which matters most on a slow core.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -Xss512k -XX:ReservedCodeCacheSize=64m -XX:+ExitOnOutOfMemoryError"

# Class Data Sharing: start the app once at build time (stopping right after the context is
# ready) and archive the loaded classes. Real starts then map that archive instead of loading
# and verifying thousands of classes again. The training run must not touch a database.
RUN SPRING_FLYWAY_ENABLED=false \
    SPRING_JPA_HIBERNATE_DDL_AUTO=none \
    SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect \
    SPRING_JPA_PROPERTIES_HIBERNATE_BOOT_ALLOW_JDBC_METADATA_ACCESS=false \
    APP_SEED_DEMO_DATA=false \
    java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar footballhub-api.jar \
    && chown app:app app.jsa

USER app
# Render sets PORT; application.yml reads it (default 8080).
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "footballhub-api.jar"]

# Build: Maven (via the wrapper) on a full JDK 21.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
# Download dependencies in their own layer so code changes rebuild quickly.
RUN ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/footballhub-api.jar extract --destination /workspace/extracted

# Run: JRE 25 (the code targets Java 21 and runs unchanged) as a non-root user. Java 25 is
# used for its AOT cache, which starts the app faster than Java 21's class data sharing.
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
COPY --from=build /workspace/extracted/ ./

# Tuned for small instances (Render free: 0.1 CPU, 512 MB, so a 360 MB heap). C1-only JIT and the serial GC
# spend far less CPU while starting up than the defaults, which matters most on a slow core.
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx360m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -Xss512k -XX:ReservedCodeCacheSize=64m -XX:+ExitOnOutOfMemoryError"

# AOT cache (JEP 483/514/515): start the app once at build time, stopping right after the
# context is ready, and save the loaded and linked classes plus method profiles. Real starts
# reuse that cache instead of redoing the work. The heap is fixed (-Xmx) because the cache
# only loads with the same heap settings it was trained with. The training run must not
# touch a database. Measured at 0.1 CPU / 512 MB: 5+ min (defaults) -> 56 s.
RUN SPRING_FLYWAY_ENABLED=false \
    SPRING_JPA_HIBERNATE_DDL_AUTO=none \
    SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect \
    SPRING_JPA_PROPERTIES_HIBERNATE_BOOT_ALLOW_JDBC_METADATA_ACCESS=false \
    APP_SEED_DEMO_DATA=false \
    java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar footballhub-api.jar \
    && chown app:app app.aot

USER app
# Render sets PORT; application.yml reads it (default 8080).
EXPOSE 8080
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "footballhub-api.jar"]

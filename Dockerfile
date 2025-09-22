# Multi-stage build für optimierte Größe
FROM maven:3.9-eclipse-temurin-17-alpine AS build

# Arbeitsverzeichnis erstellen
WORKDIR /app

# POM kopieren für besseres Caching
COPY pom.xml .

# Dependencies downloaden (wird gecacht wenn sich pom.xml nicht ändert)
RUN mvn dependency:go-offline -B

# Source Code kopieren
COPY src ./src

# Bot kompilieren
RUN mvn clean package -DskipTests

# Runtime Image
FROM eclipse-temurin:17-jre-alpine

# Non-root User erstellen für Sicherheit
RUN addgroup -g 1000 botuser && \
    adduser -u 1000 -G botuser -s /bin/sh -D botuser

# Arbeitsverzeichnis erstellen
WORKDIR /app

# JAR von Build Stage kopieren
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar

# Permissions setzen
RUN chown -R botuser:botuser /app

# Als non-root User wechseln
USER botuser

# Port exponieren (falls später Webinterface hinzugefügt wird)
EXPOSE 8080

# Healthcheck hinzufügen
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD echo "Bot läuft" || exit 1

# Bot starten
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:+UseContainerSupport", "-Xmx512m", "-jar", "app.jar"]

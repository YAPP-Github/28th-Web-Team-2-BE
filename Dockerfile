FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY looky-common ./looky-common
COPY looky-core ./looky-core
COPY looky-infrastructure ./looky-infrastructure
COPY looky-api ./looky-api

RUN chmod +x gradlew
RUN ./gradlew :looky-api:bootJar -x test --no-daemon --info

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=builder /workspace/looky-api/build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

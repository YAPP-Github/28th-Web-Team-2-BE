FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY --chown=root:root gradlew settings.gradle build.gradle ./
COPY --chown=root:root gradle ./gradle
COPY --chown=root:root looky-common ./looky-common
COPY --chown=root:root looky-core ./looky-core
COPY --chown=root:root looky-infrastructure ./looky-infrastructure
COPY --chown=root:root looky-api ./looky-api

RUN chmod +x gradlew
RUN ./gradlew :looky-api:clean :looky-api:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

RUN addgroup --system spring \
    && adduser --system --ingroup spring spring

COPY --from=builder /workspace/looky-api/build/libs/app.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

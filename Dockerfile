FROM gradle:8.14.3-jdk21-alpine AS builder

WORKDIR /workspace

COPY --chown=gradle:gradle gradlew gradlew.bat settings.gradle build.gradle ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle auth-api ./auth-api
COPY --chown=gradle:gradle feed-api ./feed-api

RUN chmod +x gradlew
RUN ./gradlew :feed-api:clean :feed-api:bootJar -x test --no-daemon --info

FROM amazoncorretto:21-alpine AS runtime

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /workspace/feed-api/build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

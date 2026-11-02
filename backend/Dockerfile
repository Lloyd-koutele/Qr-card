# Images multi-architectures : fonctionne sur ARM (Oracle Always Free) comme sur x86
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
# Cache Gradle conserve entre les builds : les dependances ne sont telechargees qu'une fois
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S madeqrcard && adduser -S madeqrcard -G madeqrcard
COPY --from=build /app/build/libs/madeqrcard-api.jar app.jar
USER madeqrcard
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# Images multi-architectures : fonctionne sur ARM (Oracle Always Free) comme sur x86
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S madeqrcard && adduser -S madeqrcard -G madeqrcard
COPY --from=build /app/target/madeqrcard-api.jar app.jar
USER madeqrcard
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S pukaar && adduser -S pukaar -G pukaar
COPY --from=build /app/target/pukaar-backend-*.jar app.jar
RUN mkdir -p /data/evidence && chown -R pukaar:pukaar /data /app
USER pukaar
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]

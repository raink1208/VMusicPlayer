# backend/Dockerfile

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .

RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 6565 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

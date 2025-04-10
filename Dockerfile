FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/MovieGradle-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/ || exit 1

CMD ["java", "-jar", "app.jar"]
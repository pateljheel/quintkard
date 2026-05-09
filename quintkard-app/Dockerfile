FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon bootJar


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd --system --create-home --uid 10001 quintkard
RUN mkdir -p /var/secrets/google

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

USER quintkard

EXPOSE 8080

ENV JAVA_OPTS=""
ENV GOOGLE_APPLICATION_CREDENTIALS=/var/secrets/google/application_default_credentials.json

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# 빌드 스테이지 — Gradle로 bootJar 생성
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies || true
COPY src src
RUN ./gradlew --no-daemon bootJar

# 실행 스테이지 — JDK 대신 JRE만 담아 이미지를 슬림화
FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --create-home --home-dir /app appuser
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

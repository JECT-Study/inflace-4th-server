FROM gradle:9.4.0-jdk25-noble AS build
WORKDIR /app

# Gradle 캐시 최적화
COPY build.gradle settings.gradle ./
COPY gradle gradle

RUN gradle dependencies --no-daemon

# 소스 복사 후 빌드
COPY src src
RUN gradle bootJar --no-daemon

# 2단계: 실행 환경, eclipse-temurin으로 가볍게
FROM eclipse-temurin:25-jdk-noble
ENV TZ=Asia/Seoul
RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul"

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
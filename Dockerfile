#  빌드를 git actions에 위임
#  실행 환경, eclipse-temurin으로 가볍게
FROM eclipse-temurin:25-jdk-noble
ENV TZ=Asia/Seoul
RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul"

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
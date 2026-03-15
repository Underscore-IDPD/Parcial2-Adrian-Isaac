FROM eclipse-temurin:25-jre
WORKDIR /app

COPY build/libs/event.jar app.jar

VOLUME /data
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "app.jar"]
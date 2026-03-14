FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/eveny.jar app.jar
VOLUME /data
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "app.jar"]
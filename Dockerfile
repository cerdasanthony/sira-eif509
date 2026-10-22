FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=America/Costa_Rica
RUN addgroup -S sira && adduser -S sira -G sira
COPY --from=build /workspace/build/libs/sira.jar /app/sira.jar
USER sira
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "/app/sira.jar"]

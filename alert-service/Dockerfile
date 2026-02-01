FROM eclipse-temurin:17-jdk

WORKDIR /app

# Download Datadog Java APM agent
ADD https://dtdg.co/latest-java-tracer /app/dd-java-agent.jar

COPY ./build/libs/alert-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8085

CMD ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "alert-service-0.0.1-SNAPSHOT.jar"]
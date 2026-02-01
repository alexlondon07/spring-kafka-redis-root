FROM eclipse-temurin:21-jdk

WORKDIR /app

# Download Datadog Java APM agent
ADD https://dtdg.co/latest-java-tracer /app/dd-java-agent.jar

COPY ./build/libs/crypto-fetcher-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8083

CMD ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "crypto-fetcher-service-0.0.1-SNAPSHOT.jar"]
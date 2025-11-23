FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY ./build/libs/crypto-fetcher-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8083

CMD ["java", "-jar", "crypto-fetcher-service-0.0.1-SNAPSHOT.jar"]
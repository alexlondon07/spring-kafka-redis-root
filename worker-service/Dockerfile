FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY ./build/libs/worker-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8081

CMD ["java", "-jar", "worker-service-0.0.1-SNAPSHOT.jar"]
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY ./build/libs/alert-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8085

CMD ["java", "-jar", "alert-service-0.0.1-SNAPSHOT.jar"]
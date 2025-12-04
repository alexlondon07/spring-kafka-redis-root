FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY ./build/libs/price-processor-service-0.0.1-SNAPSHOT.jar .

EXPOSE 8084

CMD ["java", "-jar", "price-processor-service-0.0.1-SNAPSHOT.jar"]
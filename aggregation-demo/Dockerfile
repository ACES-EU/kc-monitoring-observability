FROM eclipse-temurin:18-alpine

RUN apk --no-cache add curl

RUN mkdir /app

WORKDIR /app

EXPOSE 8080

COPY ./target/aggregation-demo-*.jar /app/aggregation-demo.jar

CMD ["java", "-jar", "aggregation-demo.jar"]

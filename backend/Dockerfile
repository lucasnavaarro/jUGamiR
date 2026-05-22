FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache python3 py3-pip && pip3 install psycopg2-binary --break-system-packages
COPY --from=build /app/target/*.jar app.jar
COPY db/*.py /app/db/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

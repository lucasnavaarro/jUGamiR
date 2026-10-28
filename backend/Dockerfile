FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-psycopg2 wget \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
COPY db/*.py /app/db/
RUN addgroup --system app && adduser --system --ingroup app --home /app --no-create-home app \
    && mkdir -p /app/static/imagenes \
    && chown -R app:app /app/static \
    && chmod -R a-w /app/db /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

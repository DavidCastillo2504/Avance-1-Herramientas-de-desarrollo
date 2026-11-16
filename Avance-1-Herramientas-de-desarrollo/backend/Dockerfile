FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
# El proceso no corre como root dentro del contenedor.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /app/target/sabor-api-0.1.0.jar app.jar
USER app
EXPOSE 8080
# MaxRAMPercentage hace que la JVM respete la memoria disponible de la instancia o del contenedor.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

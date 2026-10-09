# syntax=docker/dockerfile:1
# Imagen en dos etapas: compila con el JDK y ejecuta solo con el JRE.

FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# Usuario sin privilegios: el proceso no corre como root
RUN useradd --system --uid 10001 banco
COPY --from=compilacion /app/target/banco-*.jar app.jar
USER banco
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# Sondas para el orquestador: /actuator/health/liveness y /actuator/health/readiness
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ===== BUILD STAGE =====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== RUNTIME STAGE =====
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
CMD curl -f http://localhost:8081/optics/actuator/health || exit 1

CMD ["java","-jar","app.jar"]
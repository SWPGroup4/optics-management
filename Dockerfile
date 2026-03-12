# ===== BUILD STAGE =====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
# Tải dependencies trước để cache (tăng tốc độ build lần sau)
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== RUNTIME STAGE =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Cài đặt curl để thực hiện lệnh gọi Healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Sửa lại thành 8081 cho đúng cấu hình server.port của bạn
EXPOSE 8081

# Cấu hình Healthcheck gọi vào Actuator của Spring Boot
# Chờ 40s cho Spring Boot khởi động xong, sau đó 30s check 1 lần
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/optics/actuator/health || exit 1

CMD ["java","-jar","app.jar"]
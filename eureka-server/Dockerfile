# =========================================================
# Stage 1: Build
# =========================================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom first (layer caching)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached unless pom changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# =========================================================
# Stage 2: Runtime
# =========================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health checks (must run as root before switching user)
RUN apk add --no-cache curl

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]

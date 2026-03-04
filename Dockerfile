# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper & pom first (cache dependencies layer)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code & build
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ===== Stage 2: Run =====
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create uploads directory
RUN mkdir -p uploads/books uploads/avatars uploads/blog uploads/categories uploads/covers uploads/documents uploads/fields uploads/images

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port (Railway will override via PORT env)
EXPOSE 9090

# Default to prod profile (Railway can override via env var)
ENV SPRING_PROFILES_ACTIVE=prod

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

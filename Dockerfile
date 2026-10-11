# Multi-stage Docker build for SmartNav System
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy maven wrapper & pom for caching dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and package
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal Production JRE Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create directory for report uploads
RUN mkdir -p uploads/reports

# Copy built artifact from builder
COPY --from=builder /app/target/smart-nav-system-*.jar app.jar

# Environment setup
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

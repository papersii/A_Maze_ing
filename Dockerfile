# Use a specific OpenJDK version to ensure consistency across all machines
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy necessary gradle files first to cache dependencies
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Give execution permission to gradlew
RUN chmod +x gradlew

# Download dependencies (this step will be cached unless gradle files change)
# The 'android' task is skipped as we are focusing on the desktop/core logic consistency first
RUN ./gradlew --no-daemon dependencies

# Copy the rest of the source code
COPY . .

# Build the project
# We use 'assemble' to compile classes and build archives. 
# We skip tests here to just verify buildability, or run 'test' to verify logic.
# 'test' is better for ensuring logic consistency.
CMD ["./gradlew", "--no-daemon", "clean", "test", "desktop:dist"]

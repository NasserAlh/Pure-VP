# Use the specific Gradle image with JDK 17 on Alpine
FROM gradle:8.4.0-jdk17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the project into the container
COPY . .

# Build the project inside the container
RUN gradle build --no-daemon

# Start the container with /bin/bash
CMD ["/bin/bash"]

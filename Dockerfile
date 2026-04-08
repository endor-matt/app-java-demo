FROM eclipse-temurin:17-jdk AS build

RUN apt-get update && apt-get install -y \
    curl \
    wget \
    openssh-server \
    netcat-openbsd \
    vim \
    telnet \
    maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the Maven project files into the container
COPY . .

# Build the Maven project
RUN mvn clean install

# Use a smaller image for deployment
FROM eclipse-temurin:17-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the built artifact from the build stage
COPY --from=build /app/target/endor-java-webapp-demo.jar .

# Expose any necessary ports
EXPOSE 443

# Set the command to run your application
CMD ["java", "-jar", "endor-java-webapp-demo.jar"]

# CDC POC

A minimal Quarkus service with Debezium CDC capabilities for proof-of-concept testing.

## Features

- **Health Check**: Simple REST endpoint at `/health`
- **Debezium Integration**: Embedded engine with MongoDB connector (disabled by default)
- **Minimal Dependencies**: Only essential Quarkus and Debezium components

## Quick Start

### 1. Run without Debezium (default)
```bash
./mvnw quarkus:dev
```
Visit: http://localhost:8080/health

### 2. Run with Debezium enabled
```bash
# Start MongoDB locally first
docker run -d --name mongo -p 27017:27017 mongo:latest

# Enable Debezium and run
./mvnw quarkus:dev -Ddebezium.enabled=true
```

### 3. Test CDC Events
```bash
# Connect to MongoDB and insert test data
docker exec -it mongo mongosh
use testdb
db.testcol.insertOne({name: "test", value: 123})
```

Watch the console for CDC events logged to stdout.

## Configuration

All Debezium settings are in `src/main/resources/application.properties`:

- `debezium.enabled=false` - Master switch (set to `true` to enable)
- MongoDB connection defaults to `localhost:27017/testdb`
- Collection defaults to `testcol`
- Safe defaults prevent accidental connections

## Build & Package

```bash
# Compile
./mvnw compile

# Test
./mvnw test

# Package
./mvnw package

# Run packaged JAR
java -jar target/quarkus-app/quarkus-run.jar
```

## Environment Variables

- `DEBEZIUM_ENABLED=true` - Enable Debezium at runtime
- `QUARKUS_HTTP_PORT=8080` - Change HTTP port

## Code Structure

- `HealthResource.java` - Simple REST endpoints
- `DebeziumService.java` - Minimal CDC engine (~60 lines)
- `Application.java` - Quarkus main class
- `application.properties` - All configuration with safe defaults






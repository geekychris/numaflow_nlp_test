# Numaflow Text Enrichment Application

A production-ready Numaflow application that provides advanced text enrichment capabilities using Apache OpenNLP for Named Entity Recognition (NER) and text segmentation. Designed as a User Defined Function (UDF) for Numaflow streaming pipelines.

## 🎯 Overview

This application processes streaming text events through a Numaflow pipeline, enriching them with NLP-based insights including:
- **Text Segmentation**: Intelligent sentence and paragraph detection
- **Named Entity Recognition**: Person, location, and organization extraction
- **Metadata Enrichment**: Processing statistics and performance metrics
- **Tag-based Routing**: Smart routing of enriched, skipped, and error events

## ✨ Features

- **Numaflow Integration**: Native Mapper implementation with gRPC protocol
- **Advanced NLP Processing**: OpenNLP with intelligent fallback mechanisms
- **Event Processing**: Structured JSON event handling with multiple text fields
- **Production Ready**: Docker containerization and Kubernetes deployment
- **Monitoring**: Health checks, metrics, and comprehensive logging
- **Graceful Degradation**: Continues operation when ML models unavailable
- **Tag-based Routing**: Automatic message routing based on processing results

## 🏗️ Architecture

### Design Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Kafka Source  │───▶│  Enrichment UDF  │───▶│  Kafka Sinks    │
│  (input-events) │    │   (gRPC Server)  │    │  (3 topics)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Spring Services │
                    │  ├─EventEnrich   │
                    │  ├─NLP Service   │
                    │  └─OpenNLP       │
                    └──────────────────┘
```

## Prerequisites

- **Java 21+** (Amazon Corretto 21 recommended)
- **Maven 3.9+**
- **Docker** (for containerization)
- **Kubernetes cluster** with Numaflow installed
- **Kafka cluster** (for message streaming)

## Project Structure

```
.
├── src/
│   ├── main/java/com/example/numaflow/
│   │   ├── model/              # Data models (Event, EnrichedEvent, etc.)
│   │   ├── service/            # Business logic (NLP, enrichment services)
│   │   ├── vertex/             # Numaflow vertex implementation
│   │   ├── config/             # Spring configuration
│   │   └── NumaflowEnrichmentApplication.java
│   ├── main/resources/
│   │   ├── application*.yml    # Configuration files
│   │   └── models/             # OpenNLP model files (downloaded)
│   └── test/                   # Unit and integration tests
├── docker/
├── k8s/                        # Kubernetes manifests
├── scripts/                    # Build and utility scripts
└── models/                     # NLP models directory
```

### Core Components

1. **Numaflow Integration** (`com.example.numaflow.vertex`)
   - `EnrichmentVertex`: Main Numaflow Mapper implementation
   - `StandaloneEnrichmentMapper`: Standalone UDF for containerized deployment

2. **Event Models** (`com.example.numaflow.model`)
   - `Event`: Input event structure with text fields
   - `EnrichedEvent`: Output with enrichment data and metadata
   - `TextSegment`: Individual processed text segments
   - `NamedEntity`: Extracted named entities with confidence scores

3. **Processing Services** (`com.example.numaflow.service`)
   - `EventEnrichmentService`: Main orchestration and batch processing
   - `NlpEnrichmentService`: OpenNLP integration with fallback implementations

4. **Application Layer** (`com.example.numaflow`)
   - `NumaflowEnrichmentApplication`: Spring Boot main application
   - REST controllers for testing and monitoring

### Message Flow

1. **Input**: JSON events from Kafka topic `input-events`
2. **Processing**: Text enrichment via NLP services
3. **Routing**: Tag-based routing to appropriate sinks:
   - `enriched` → `enriched-events` topic
   - `skipped` → `skipped-events` topic  
   - `error` → `error-events` topic

## 🚀 Quick Start

### Build and Test

```bash
# Clone and build
git clone <repository-url>
cd numaflow-enrichment-app

# Run all tests
mvn clean test

# Build application
mvn clean package

# Build Docker image
docker build -t numaflow-enrichment-app:latest .
```

**Expected Test Results:**
```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
✅ All tests passing!
```

## 🏃‍♂️ Running the Application

### Option 1: Standalone Development Mode

```bash
# Run Spring Boot application with REST endpoints
mvn spring-boot:run

# Application available at:
# - REST API: http://localhost:8080
# - Health: http://localhost:8080/actuator/health
# - Metrics: http://localhost:8080/actuator/metrics
```

### Option 2: Standalone Numaflow UDF Mode

```bash
# Run as standalone gRPC server (Numaflow UDF)
java -cp target/classes:target/dependency/* \
  com.example.numaflow.StandaloneEnrichmentMapper

# Server starts on port 8443 (gRPC)
# Ready for Numaflow pipeline connection
```

### Option 3: Docker Container

```bash
# Run containerized UDF
docker run -p 8443:8443 numaflow-enrichment-app:latest

# With custom configuration
docker run -p 8443:8443 \
  -e JAVA_OPTS="-Xmx1g" \
  -e LOG_LEVEL=DEBUG \
  numaflow-enrichment-app:latest
```

## 🛠️ Configuration

### Application Configuration

```yaml
# application.yml
spring:
  application:
    name: numaflow-enrichment-app
  profiles:
    active: production

logging:
  level:
    com.example.numaflow: INFO
    io.numaproj: DEBUG

# Numaflow UDF Configuration
numaflow:
  udf:
    server:
      port: 8443
      info-file: "/tmp/numaflow-server-info"

# NLP Processing Configuration
nlp:
  models:
    path: "/app/models"
    download-on-startup: false
  processing:
    batch-size: 100
    timeout-ms: 5000
```

### Environment Variables

```bash
# JVM Options
JAVA_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Application Settings
SPRING_PROFILES_ACTIVE=production
LOG_LEVEL=INFO

# NLP Configuration
NLP_MODELS_PATH=/app/models
NLP_DOWNLOAD_MODELS=false

# Numaflow Settings
NUMAFLOW_UDF_PORT=8443
NUMAFLOW_INFO_FILE=/tmp/numaflow-server-info
```

## 📊 API Endpoints (Development Mode)

### Event Enrichment

```bash
# Enrich single event
curl -X POST http://localhost:8080/api/v1/events/enrich \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Apple announces new iPhone",
    "description": "Apple Inc. unveiled the new iPhone at their Cupertino headquarters."
  }'

# Batch enrichment
curl -X POST http://localhost:8080/api/v1/events/enrich/batch \
  -H "Content-Type: application/json" \
  -d '[{"title": "Event 1"}, {"title": "Event 2"}]'
```

### Health and Monitoring

```bash
# Application health
curl http://localhost:8080/actuator/health

# Processing metrics
curl http://localhost:8080/actuator/metrics/numaflow.enrichment.processed

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## Data Models

### Input Event

```json
{
  "id": "event-123",
  "title": "Event title text",
  "description": "Event description text", 
  "content": "Optional additional content",
  "summary": "Optional summary text",
  "timestamp": "2024-01-15T10:00:00Z",
  "metadata": {}
}
```

### Enriched Event Output

```json
{
  "originalEvent": { /* original event */ },
  "enrichedFields": {
    "title": [
      {
        "text": "Event title text",
        "startIndex": 0,
        "endIndex": 17,
        "segmentNumber": 1,
        "namedEntities": [
          {
            "text": "Event",
            "type": "ORGANIZATION",
            "startIndex": 0,
            "endIndex": 5,
            "confidence": 0.85
          }
        ]
      }
    ]
  },
  "processingTimestamp": "2024-01-15T10:00:01Z",
  "enrichmentMetadata": {
    "processedFields": ["title", "description"],
    "processingTimeMs": 150,
    "totalSegments": 3,
    "totalNamedEntities": 5,
    "nlpModelsUsed": "OpenNLP"
  }
}
```

## Test Data Generator

The application includes a built-in REST API for generating realistic test events. This is especially useful for testing the enrichment pipeline with various event types and volumes.

### Quick Start

```bash
# Start the application
mvn spring-boot:run

# Generate a few sample events (easiest way to get started)
curl http://localhost:8080/api/test/sample
```

### Generator Configuration

The test data generator can be configured in `application.yml`:

```yaml
app:
  test-data-generator:
    max-count: 10000          # Maximum events per request
    max-rate-per-second: 1000 # Maximum events per second
    default-topic: events     # Default Kafka topic
    scheduler-pool-size: 2    # Thread pool for rate limiting
```

### REST API Usage

#### 1. Generate Sample Events (Quickest)

```bash
# Get a small batch of sample events (no Kafka publishing)
curl http://localhost:8080/api/test/sample
```

Response:
```json
[
  {
    "id": "b8c9d1e2-f3a4-5b6c-7d8e-9f0a1b2c3d4e",
    "title": "Apple Inc. announces breakthrough in artificial intelligence technology",
    "content": "The technology giant Apple Inc., based in Cupertino California...",
    "description": "Tech News: The technology giant Apple Inc., based in Cupertino California, announced today that its quarterly results exceeded expectations.",
    "timestamp": "2024-01-15T10:30:45.123Z",
    "metadata": {
      "eventType": "Tech News",
      "generated": true,
      "generator": "TestDataGenerator"
    }
  }
]
```

#### 2. Generate Batch of Events (Immediate)

Generate events immediately and publish to Kafka:

```bash
# Generate 5 events immediately to default topic (events)
curl -X POST "http://localhost:8080/api/test/generate-batch" \
  -H "Content-Type: application/json" \
  -d '{"count": 5}'

# Generate 10 events to a specific topic
curl -X POST "http://localhost:8080/api/test/generate-batch" \
  -H "Content-Type: application/json" \
  -d '{"count": 10, "topic": "test-events"}'
```

Response:
```json
{
  "message": "Successfully generated 5 test events",
  "result": {
    "successCount": 5,
    "errorCount": 0,
    "totalCount": 5,
    "timestamp": "2024-01-15T10:30:45.123",
    "hasErrors": false
  }
}
```

#### 3. Generate Events at Specific Rate (Advanced)

Generate events over time at a controlled rate:

```bash
# Generate 100 events at 10 events per second (takes 10 seconds)
curl -X POST "http://localhost:8080/api/test/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 100,
    "ratePerSecond": 10.0,
    "topic": "events"
  }'

# Generate 50 events slowly (1 event every 2 seconds)
curl -X POST "http://localhost:8080/api/test/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 50,
    "ratePerSecond": 0.5,
    "topic": "events"
  }'
```

#### 4. Get Generator Information

```bash
# Check generator capabilities and limits
curl http://localhost:8080/api/test/info
```

Response:
```json
{
  "service": "Test Data Generator",
  "description": "Generates realistic test events for the enrichment pipeline",
  "limits": {
    "maxCount": 10000,
    "maxRatePerSecond": 1000
  },
  "defaultTopic": "events",
  "eventTypes": [
    "Tech News",
    "Business Update", 
    "Conference Event",
    "Market Analysis",
    "Research News",
    "International Event",
    "Sports News",
    "Healthcare Innovation"
  ]
}
```

### Generated Event Types

The generator creates realistic events across different categories:

- **Tech News**: Apple, Microsoft, Google announcements
- **Business Updates**: Partnerships, quarterly reports
- **Conference Events**: Speaking engagements, presentations
- **Market Analysis**: Stock movements, financial reports
- **Research News**: University studies, breakthroughs
- **International Events**: Global summits, diplomatic meetings
- **Sports News**: Games, tournaments, championships
- **Healthcare Innovation**: FDA approvals, medical advances

### Complete Testing Workflow

Here's a complete workflow for testing the enrichment pipeline on macOS:

```bash
# 1. Start Kafka (using Docker)
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest

# 2. Create required topics
docker exec kafka kafka-topics \
  --create --bootstrap-server localhost:9092 \
  --topic events --partitions 1 --replication-factor 1

docker exec kafka kafka-topics \
  --create --bootstrap-server localhost:9092 \
  --topic enriched-events --partitions 1 --replication-factor 1

# 3. Start the application 
mvn spring-boot:run

# 4. Start monitoring enriched events (in another terminal)
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic enriched-events \
  --from-beginning

# 5. Generate test data
# Start with samples
curl http://localhost:8080/api/test/sample

# Generate immediate batch
curl -X POST "http://localhost:8080/api/test/generate-batch" \
  -H "Content-Type: application/json" \
  -d '{"count": 5}'

# Generate at controlled rate for load testing
curl -X POST "http://localhost:8080/api/test/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 100,
    "ratePerSecond": 5.0,
    "topic": "events"
  }'
```

### Monitoring and Verification

```bash
# Check if events are being processed
curl http://localhost:8080/actuator/metrics/kafka.consumer.records-consumed-total

# Monitor Kafka topics
kafka-topics --list --bootstrap-server localhost:9092

# Check event counts in topics
kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic events

kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic enriched-events
```

### Performance Testing

```bash
# Generate high-volume test data for performance testing
curl -X POST "http://localhost:8080/api/test/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 1000,
    "ratePerSecond": 50.0,
    "topic": "events"
  }'

# Monitor application performance
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/process.cpu.usage
```

### Error Handling

The generator handles various error scenarios:

```bash
# Test with invalid parameters (too many events)
curl -X POST "http://localhost:8080/api/test/generate-batch" \
  -H "Content-Type: application/json" \
  -d '{"count": 99999}'

# Response: HTTP 400 Bad Request
# {"error": "Count exceeds maximum limit of 10000"}

# Test with invalid rate
curl -X POST "http://localhost:8080/api/test/generate" \
  -H "Content-Type: application/json" \
  -d '{"count": 10, "ratePerSecond": -1}'

# Response: HTTP 400 Bad Request  
# {"error": "Rate must be positive"}
```

### Integration with CI/CD

You can integrate the generator into automated testing:

```bash
#!/bin/bash
# test-pipeline.sh

echo "Starting enrichment pipeline test..."

# Start application (assumes it's already running)
echo "Generating test data..."
RESPONSE=$(curl -s -X POST "http://localhost:8080/api/test/generate-batch" \
  -H "Content-Type: application/json" \
  -d '{"count": 10}')

echo "Generator response: $RESPONSE"

# Wait for processing
sleep 5

# Check if events were processed
PROCESSED=$(curl -s "http://localhost:8080/actuator/metrics/kafka.consumer.records-consumed-total" | jq -r '.measurements[0].value')

if [ "$PROCESSED" -gt 0 ]; then
  echo "✓ Pipeline test passed - $PROCESSED events processed"
  exit 0
else
  echo "✗ Pipeline test failed - no events processed"
  exit 1
fi
```

## ☸️ Kubernetes Deployment

### Deploy Complete Numaflow Pipeline

```bash
# 1. Install Numaflow (if not already installed)
kubectl create namespace numaflow-system
kubectl apply -f https://github.com/numaproj/numaflow/releases/latest/download/install.yaml

# 2. Deploy Kafka (for development)
kubectl apply -f k8s/kafka-deployment.yaml

# 3. Create Kafka topics
kubectl exec -it kafka-0 -- kafka-topics.sh \
  --create --topic input-events --bootstrap-server localhost:9092
kubectl exec -it kafka-0 -- kafka-topics.sh \
  --create --topic enriched-events --bootstrap-server localhost:9092
kubectl exec -it kafka-0 -- kafka-topics.sh \
  --create --topic skipped-events --bootstrap-server localhost:9092
kubectl exec -it kafka-0 -- kafka-topics.sh \
  --create --topic error-events --bootstrap-server localhost:9092

# 4. Build and push Docker image
docker build -t your-registry/numaflow-enrichment-app:latest .
docker push your-registry/numaflow-enrichment-app:latest

# 5. Update image reference in pipeline manifest
# Edit k8s/numaflow-pipeline.yaml to use your image

# 6. Deploy the pipeline
kubectl apply -f k8s/numaflow-pipeline.yaml
```

### Monitor Pipeline

```bash
# Check pipeline status
kubectl get pipeline enrichment-pipeline

# View vertex status
kubectl get vertices

# Check logs
kubectl logs -l app=enrichment-vertex -f

# Monitor processing metrics
kubectl port-forward svc/enrichment-vertex 8080:8080
curl http://localhost:8080/actuator/metrics
```

### Send Test Messages

```bash
# Send test event to input topic
kubectl exec -it kafka-0 -- kafka-console-producer.sh \
  --topic input-events --bootstrap-server localhost:9092

# Paste JSON event:
{
  "title": "John Doe speaks at Microsoft conference in Seattle",
  "description": "Software engineer John Doe will present research on AI at the Microsoft developer conference in Seattle, Washington.",
  "timestamp": "2025-09-15T22:00:00Z",
  "id": "test-001"
}

# Monitor enriched output
kubectl exec -it kafka-0 -- kafka-console-consumer.sh \
  --topic enriched-events --bootstrap-server localhost:9092 --from-beginning
```

## Development

### Building

```bash
# Clean build
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Skip tests during packaging
mvn package -DskipTests
```

## 🧪 Testing

### Run All Tests

```bash
# Unit and integration tests
mvn test

# Specific test suite
mvn test -Dtest=EnrichmentIntegrationTest
mvn test -Dtest=EventEnrichmentServiceTest
mvn test -Dtest=NlpEnrichmentServiceTest
```

### Test Coverage

- **EnrichmentIntegrationTest**: End-to-end Numaflow integration
- **EventEnrichmentServiceTest**: Service layer functionality
- **NlpEnrichmentServiceTest**: NLP processing capabilities
- **Performance Tests**: Load and stress testing scenarios

### Manual Testing with Numaflow

```bash
# 1. Start local UDF server
java -cp target/classes:target/dependency/* \
  com.example.numaflow.StandaloneEnrichmentMapper

# 2. Use Numaflow CLI to test
numaflow udf test \
  --server localhost:8443 \
  --payload '{"title":"Test Event","description":"Test description"}'
```

### Code Quality

```bash
# Check code style (if configured)
mvn checkstyle:check

# Run static analysis (if configured)
mvn spotbugs:check

# Check for dependency vulnerabilities
mvn org.owasp:dependency-check-maven:check
```

## Troubleshooting

### Common Issues

1. **NLP Models Not Loading**
   - Check model files exist in `/app/models`
   - Verify file permissions
   - Check application logs for model loading errors
   - Fallback implementation will be used if models fail to load

2. **High Memory Usage**
   - NLP models require significant memory
   - Adjust JVM heap settings: `-Xms512m -Xmx1024m`
   - Consider reducing batch size in configuration

3. **Slow Processing**
   - Check if proper NLP models are loaded (not fallback)
   - Monitor CPU usage and adjust resource limits
   - Consider horizontal scaling

4. **Kafka Connection Issues**
   - Verify Kafka broker addresses
   - Check network connectivity
   - Validate topic names and permissions

### Debugging

```bash
# Enable debug logging
export LOGGING_LEVEL_COM_EXAMPLE_NUMAFLOW=DEBUG

# Check health endpoints
curl http://localhost:8080/actuator/health

# View detailed metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Performance Tuning

1. **JVM Settings**
   ```bash
   JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
   ```

2. **Application Settings**
   ```yaml
   app:
     processing:
       batch-size: 200  # Increase for better throughput
       timeout-ms: 60000  # Adjust timeout as needed
   ```

3. **Resource Allocation**
   - CPU: 250m-500m per replica
   - Memory: 512Mi-1Gi per replica
   - Consider node affinity for consistent performance

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review application logs
3. Create an issue in the repository
4. Contact the development team

## References

- [Numaflow Documentation](https://numaflow.numaproj.io/)
- [OpenNLP Documentation](https://opennlp.apache.org/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

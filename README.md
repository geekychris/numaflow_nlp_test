# Numaflow Text Enrichment Application

A Spring Boot application that enriches Kafka events with NLP processing using Numaflow. This application consumes JSON events from Kafka, performs text segmentation and named entity recognition, and outputs enriched events.

## Features

- **Text Segmentation**: Splits text into sentences using OpenNLP
- **Named Entity Recognition**: Identifies persons, locations, and organizations
- **Local NLP Models**: Uses OpenNLP models that run locally (no external API calls)
- **Test Data Generation**: Built-in REST API for generating realistic test events with configurable rates
- **Numaflow Integration**: Implements Numaflow vertex interface for stream processing
- **Fallback Implementation**: Graceful degradation when NLP models are unavailable
- **Production Ready**: Includes monitoring, health checks, and scaling configuration

## Architecture

```
Kafka Topic (events) 
    ↓
Numaflow Pipeline
    ↓
Text Enrichment Vertex (this application)
    ↓
Kafka Topic (enriched-events)
```

## Prerequisites

- Java 21 or higher (Amazon Corretto 23 recommended)
- Maven 3.9+
- Docker and Docker Compose (for local testing)
- Kubernetes cluster with Numaflow installed (for production deployment)

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

## Quick Start

### 1. Local Development Setup

```bash
# Clone the repository
git clone <repository-url>
cd numaflow-enrichment-app

# Download NLP models (optional - fallback implementation available)
chmod +x scripts/download-models.sh
./scripts/download-models.sh

# Build the application
mvn clean package

# Run tests
mvn test

# Run the application locally
mvn spring-boot:run
```

### 2. Local Testing with Docker Compose

```bash
# Start the complete stack (Kafka + Application)
docker-compose up -d

# Check service status
docker-compose ps

# View application logs
docker-compose logs -f enrichment-app

# Access Kafka UI
open http://localhost:8090

# Health check
curl http://localhost:8080/actuator/health
```

### 3. Testing the Application

Once the stack is running, you can test it by sending events to Kafka:

```bash
# Create test event
echo '{
  "id": "test-001",
  "title": "John Doe speaks at Microsoft conference in Seattle",
  "description": "Software engineer John Doe will present his research on artificial intelligence at the Microsoft developer conference held in Seattle, Washington.",
  "timestamp": "2024-01-15T10:00:00Z"
}' > test-event.json

# Send to Kafka (using docker-compose kafka container)
docker-compose exec kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic events < test-event.json

# Monitor enriched events
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic enriched-events \
  --from-beginning
```

## Configuration

### Application Profiles

- `default`: Local development
- `test`: Unit testing
- `docker`: Docker container deployment  
- `kubernetes`: Kubernetes deployment

### Key Configuration Properties

```yaml
app:
  nlp:
    fallback-enabled: true
    models:
      sentence-model: classpath:models/en-sent.bin
      tokenizer-model: classpath:models/en-token.bin
      ner-models:
        person: classpath:models/en-ner-person.bin
        location: classpath:models/en-ner-location.bin
        organization: classpath:models/en-ner-organization.bin
  processing:
    batch-size: 100
    timeout-ms: 30000
```

## API Endpoints

### Management Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application information |
| `/actuator/metrics` | Application metrics |
| `/actuator/prometheus` | Prometheus metrics |

### Test Data Generator Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/test/generate` | POST | Generate test events at specified rate |
| `/api/test/generate-batch` | POST | Generate batch of events immediately |
| `/api/test/sample` | GET | Generate small sample of test events |
| `/api/test/info` | GET | Get generator service information |

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

## Kubernetes Deployment

### Prerequisites

1. Kubernetes cluster with Numaflow installed
2. Kafka running in the cluster
3. Container registry access

### Build and Push Image

```bash
# Build Docker image
docker build -t numaflow/text-enrichment-app:latest .

# Tag for your registry
docker tag numaflow/text-enrichment-app:latest your-registry/text-enrichment-app:latest

# Push to registry
docker push your-registry/text-enrichment-app:latest
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace numaflow-system

# Deploy the application
kubectl apply -f k8s/deployment.yaml

# Deploy the Numaflow pipeline
kubectl apply -f k8s/numaflow-pipeline.yaml

# Check deployment status
kubectl get pods -n numaflow-system
kubectl get pipeline -n numaflow-system
```

### Monitor the Pipeline

```bash
# Check pipeline status
kubectl describe pipeline text-enrichment-pipeline -n numaflow-system

# View application logs
kubectl logs -f deployment/text-enrichment-app -n numaflow-system

# Check metrics
kubectl port-forward svc/text-enrichment-service 8080:80 -n numaflow-system
curl http://localhost:8080/actuator/metrics
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

### Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn test -Dtest="*IntegrationTest"

# Run specific test class
mvn test -Dtest="NlpEnrichmentServiceTest"

# Generate test coverage report
mvn jacoco:report
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

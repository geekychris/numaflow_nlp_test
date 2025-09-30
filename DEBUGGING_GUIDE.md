# Hybrid Debugging Guide: IntelliJ + Kubernetes

This guide explains how to run your Numaflow enrichment application locally in IntelliJ for debugging while keeping the Numaflow infrastructure running in Kubernetes.

## 🎯 Overview

The challenge with debugging Numaflow UDFs is that they're designed to run within the Numaflow ecosystem in Kubernetes. This setup allows you to:

- **Debug locally** in IntelliJ with full IDE support
- **Keep infrastructure** (Kafka, Numaflow) running in Kubernetes
- **Test the same code** that runs in production
- **Use familiar tools** like breakpoints, variable inspection, etc.

## 🏗️ Architecture

```
┌─────────────────────┐    ┌──────────────────────┐    ┌─────────────────────┐
│   IntelliJ (Local)  │    │   Port Forwarding    │    │   Kubernetes        │
│                     │    │                      │    │                     │
│  ┌───────────────┐  │    │  localhost:9092 ────┼────┼──▶ kafka:9092       │
│  │ Spring Boot   │  │    │  localhost:8443 ────┼────┼──▶ numaflow:8443    │
│  │ Local Mode    │  │    │  localhost:8081 ────┼────┼──▶ monitoring:80    │
│  │               │  │    │                      │    │                     │
│  │ REST API      │  │    │                      │    │                     │
│  │ Debug Mode    │  │    │                      │    │                     │
│  └───────────────┘  │    │                      │    │                     │
└─────────────────────┘    └──────────────────────┘    └─────────────────────┘
```

## 🚀 Quick Start

### 1. Choose Your Setup

You have two options for the messaging layer:

**Option A: Use Kubernetes Kafka (Recommended)**
```bash
# Set up port forwarding to K8s Kafka
./scripts/setup-k8s-forwarding.sh
```

**Option B: Use Local Kafka**
```bash
# Start local Kafka with Docker
docker-compose -f docker-compose-local.yml up -d
```

### 2. Run in IntelliJ

1. **Open the project** in IntelliJ IDEA
2. **Select the run configuration**: "Local Development (Debug)"
3. **Set breakpoints** in your code
4. **Run with Debug** (Shift+F9)

### 3. Test Your Code

Once running, test via REST endpoints:

```bash
# Test the debug endpoints
curl http://localhost:8080/api/debug/info

# Simulate Numaflow processing
curl -X POST http://localhost:8080/api/debug/numaflow/process \
  -H "Content-Type: application/json" \
  -d '{"id":"test-1","title":"John Doe works at Apple Inc in California"}'

# Direct enrichment (bypass Numaflow simulation)
curl -X POST http://localhost:8080/api/debug/direct/enrich \
  -H "Content-Type: application/json" \
  -d '{"id":"test-2","title":"Microsoft announces new features"}'
```

## 🔧 Detailed Setup Instructions

### Prerequisites

- **Java 21+** (Amazon Corretto 23 as configured)
- **IntelliJ IDEA** with Maven support
- **Docker** (for local Kafka option)
- **kubectl** (for K8s port forwarding option)
- **Access to Kubernetes cluster** with Numaflow installed

### Step 1: Build the Project

```bash
# Clean build to ensure everything compiles
mvn clean compile

# Run tests to verify functionality
mvn test

# Package (optional, for completeness)
mvn package -DskipTests
```

### Step 2A: Setup with Kubernetes Kafka

If you want to use the same Kafka that your Numaflow pipeline uses:

```bash
# Make scripts executable (if not already)
chmod +x scripts/*.sh

# Setup port forwarding
./scripts/setup-k8s-forwarding.sh
```

This will:
- Forward `localhost:9092` → Kubernetes Kafka
- Forward `localhost:8443` → Numaflow Dashboard (if available)
- Forward `localhost:8081` → Enrichment Service monitoring (if running)
- Save PIDs for easy cleanup

### Step 2B: Setup with Local Kafka

If you prefer a completely local setup:

```bash
# Start local Kafka cluster
docker-compose -f docker-compose-local.yml up -d

# Check status
docker-compose -f docker-compose-local.yml ps

# View Kafka UI (optional)
open http://localhost:8090
```

This provides:
- Kafka at `localhost:9092`
- Kafka UI at `localhost:8090`
- Pre-created topics: `events`, `enriched-events`, `skipped-events`, `error-events`

### Step 3: Configure IntelliJ

The project includes pre-configured run configurations:

#### Option 1: Use Pre-configured Run Configuration
1. Open IntelliJ IDEA
2. Go to **Run** → **Edit Configurations**
3. You should see "Local Development (Debug)" configuration
4. Select it and click **Debug** (or press Shift+F9)

#### Option 2: Create Manual Configuration
1. **Run** → **Edit Configurations** → **+** → **Application**
2. **Name**: `Local Development Debug`
3. **Main class**: `com.example.numaflow.LocalDevelopmentApplication`
4. **VM options**: `-Xmx1g -Xms512m -XX:+UseG1GC -Dspring.profiles.active=local`
5. **Environment variables**: `SPRING_PROFILES_ACTIVE=local`
6. **JRE**: Use Project SDK (Java 23)

### Step 4: Verify the Setup

1. **Application should start** with output showing:
   ```
   🚀 numaflow-enrichment-app-local started successfully!
   🌐 REST API: http://localhost:8080
   📊 Health: http://localhost:8080/actuator/health
   📈 Metrics: http://localhost:8080/actuator/metrics
   🧪 Test API: http://localhost:8080/api/test/sample
   ```

2. **Test the health endpoint**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. **Test debug endpoints**:
   ```bash
   curl http://localhost:8080/api/debug/info
   ```

## 🧪 Testing and Debugging

### Debug Endpoints

The application provides special debugging endpoints when running in local mode:

#### 1. Simulate Numaflow Processing
```bash
curl -X POST http://localhost:8080/api/debug/numaflow/process \
  -H "Content-Type: application/json" \
  -d '{
    "id": "debug-1",
    "title": "Apple Inc announces new iPhone in Cupertino California",
    "description": "Tim Cook presented the new features at Apple Park"
  }'
```

This endpoint:
- Calls the **exact same** `processMessage` method that Numaflow would call
- Simulates Numaflow's `Datum` input
- Returns the same output format with tags for routing
- Perfect for debugging the actual UDF code

#### 2. Direct Enrichment
```bash
curl -X POST http://localhost:8080/api/debug/direct/enrich \
  -H "Content-Type: application/json" \
  -d '{
    "id": "debug-2",
    "title": "Microsoft partners with OpenAI",
    "description": "The collaboration focuses on AI development"
  }'
```

This endpoint:
- Bypasses the Numaflow layer
- Calls enrichment services directly
- Useful for debugging business logic without UDF complexity

#### 3. Generate Test Data
```bash
# Generate sample events
curl http://localhost:8080/api/test/sample

# Generate events to Kafka
curl -X POST http://localhost:8080/api/test/generate-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 5}'
```

### Setting Breakpoints

1. **Open your Java files** in IntelliJ
2. **Click in the gutter** next to line numbers to set breakpoints
3. **Recommended breakpoint locations**:
   - `EnrichmentVertex.processMessage()` - Main Numaflow entry point
   - `EventEnrichmentService.enrichEvent()` - Business logic
   - `NlpEnrichmentService.enrichText()` - NLP processing
   - `DebugController` methods - Debug endpoints

### Debugging Tips

1. **Start with debug endpoints** - they're easier to trigger than full message flow
2. **Use conditional breakpoints** for specific event IDs
3. **Watch variables** like `EnrichedEvent`, `TextSegment`, `NamedEntity`
4. **Step through** the exact code that runs in production

## 🔄 Message Flow Testing

### With Kubernetes Kafka

If using K8s port forwarding, you can test the full message flow:

```bash
# Send message to Kafka (requires kafka CLI tools)
echo '{"id":"flow-test-1","title":"Netflix launches new series in Los Angeles"}' | \
  kafka-console-producer --broker-list localhost:9092 --topic events

# Monitor enriched output
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic enriched-events --from-beginning
```

### With Local Kafka

```bash
# Using Docker exec
docker exec -it numaflow-kafka kafka-console-producer.sh \
  --broker-list localhost:9092 --topic events

# Paste your JSON event and press Enter:
{"id":"local-test-1","title":"Google announces new AI model in Mountain View California"}

# In another terminal, monitor output:
docker exec -it numaflow-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic enriched-events --from-beginning
```

## 📊 Monitoring and Observability

### Application Metrics
```bash
# Health check
curl http://localhost:8080/actuator/health

# All metrics
curl http://localhost:8080/actuator/metrics

# Specific metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/process.cpu.usage
```

### Kafka Monitoring

**Local Kafka UI**: http://localhost:8090 (if using local Docker setup)

**Command Line**:
```bash
# List topics
kafka-topics --list --bootstrap-server localhost:9092

# Check consumer group status
kafka-consumer-groups --bootstrap-server localhost:9092 --list
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group text-enrichment-local
```

### Kubernetes Monitoring

```bash
# Check K8s resources
kubectl get pods -n numaflow-system
kubectl get pipelines -n numaflow-system

# View pipeline status
kubectl describe pipeline text-enrichment-pipeline -n numaflow-system
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```
Error: bind: address already in use
```

**Solution**:
```bash
# Check what's using the port
lsof -i :8080
lsof -i :9092

# Kill processes if needed
kill -9 <PID>

# Or use cleanup script
./scripts/cleanup-k8s-forwarding.sh
```

#### 2. Cannot Connect to Kafka
```
Error: Connection to node -1 could not be established
```

**Solutions**:
- **For K8s Kafka**: Ensure port forwarding is running: `ps aux | grep "kubectl port-forward"`
- **For Local Kafka**: Ensure Docker containers are running: `docker-compose ps`
- **Check connectivity**: `telnet localhost 9092`

#### 3. NLP Models Not Loading
```
WARN: Could not load NLP model
```

**Solutions**:
- Models will use fallback mode (this is okay for debugging)
- To use real models: Download OpenNLP models to `src/main/resources/models/`
- Or set `app.nlp.fallback-enabled: true` (already enabled in local profile)

#### 4. Kubernetes Connection Issues
```
Error: The connection to the server was refused
```

**Solutions**:
- Check kubectl context: `kubectl config current-context`
- Verify cluster access: `kubectl cluster-info`
- Check VPN/network connectivity

#### 5. Spring Profile Not Loading
```
Using default profile instead of 'local'
```

**Solutions**:
- Check VM options include: `-Dspring.profiles.active=local`
- Check environment variable: `SPRING_PROFILES_ACTIVE=local`
- Verify `application-local.yml` exists

### Debug Logging

Enable more detailed logging by adding to VM options:
```
-Dlogging.level.com.example.numaflow=DEBUG
-Dlogging.level.io.numaproj.numaflow=DEBUG
-Dlogging.level.org.springframework.kafka=DEBUG
```

### Memory Issues

If you encounter OutOfMemoryError:
```
# Increase heap size in VM options
-Xmx2g -Xms1g
```

## 🔄 Development Workflow

### Typical Debugging Session

1. **Start the setup**:
   ```bash
   # Option A: K8s Kafka
   ./scripts/setup-k8s-forwarding.sh
   
   # Option B: Local Kafka
   docker-compose -f docker-compose-local.yml up -d
   ```

2. **Run in IntelliJ**:
   - Select "Local Development (Debug)" configuration
   - Click Debug button (Shift+F9)

3. **Set breakpoints** in your code

4. **Trigger processing**:
   ```bash
   # Test debug endpoint
   curl -X POST http://localhost:8080/api/debug/direct/enrich \
     -H "Content-Type: application/json" \
     -d '{"id":"debug","title":"Test with your data"}'
   ```

5. **Debug and iterate**:
   - Step through code
   - Inspect variables
   - Modify code
   - Hot reload (IntelliJ supports this)
   - Test again

6. **Test full flow** (optional):
   ```bash
   # Send to Kafka and verify processing
   curl -X POST http://localhost:8080/api/test/generate-batch \
     -H "Content-Type: application/json" \
     -d '{"count": 1}'
   ```

7. **Cleanup**:
   ```bash
   # Stop port forwarding
   ./scripts/cleanup-k8s-forwarding.sh
   
   # Or stop local Kafka
   docker-compose -f docker-compose-local.yml down
   ```

### Code Changes

When you modify the code:

1. **IntelliJ** will automatically compile changes
2. **Spring Boot DevTools** will restart the application (if configured)
3. **Or manually restart** the debug session

The beauty of this setup is that you're debugging the **exact same code** that runs in production!

## 🚢 Deploying to Production

Once you've debugged and tested locally:

1. **Build the Docker image**:
   ```bash
   docker build -t your-registry/numaflow-enrichment-app:latest .
   ```

2. **Update Kubernetes manifests** with your image:
   ```bash
   # Edit k8s/numaflow-pipeline.yaml
   # Update the image reference
   ```

3. **Deploy to Kubernetes**:
   ```bash
   kubectl apply -f k8s/numaflow-pipeline.yaml
   ```

4. **Monitor the deployment**:
   ```bash
   kubectl get pods -n numaflow-system
   kubectl logs -f deployment/text-enrichment-app -n numaflow-system
   ```

## 📚 Additional Resources

### Numaflow Documentation
- [Numaflow Official Docs](https://numaflow.numaproj.io/)
- [Java SDK Documentation](https://github.com/numaproj/numaflow-java)

### Development Tools
- [Kafka UI](http://localhost:8090) (when using local Docker)
- [Spring Boot Actuator](http://localhost:8080/actuator) (monitoring endpoints)

### Useful Commands

```bash
# Check what's running on ports
netstat -an | grep -E ":(8080|9092|8443)"

# View Docker logs
docker-compose -f docker-compose-local.yml logs -f kafka

# Check Kubernetes resources
kubectl get all -n numaflow-system

# Port forward manually
kubectl port-forward svc/kafka 9092:9092 -n numaflow-system
```

---

## 🎉 Success!

You now have a powerful hybrid development setup that gives you:

✅ **Full IntelliJ debugging** with breakpoints, variable inspection, and hot reload  
✅ **Production-like environment** with real Kafka and Kubernetes services  
✅ **Easy testing** via REST endpoints that simulate exact Numaflow behavior  
✅ **Flexible setup** supporting both local and Kubernetes infrastructure  
✅ **Same codebase** running in both development and production

Happy debugging! 🐛🔍
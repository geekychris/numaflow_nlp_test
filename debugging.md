# Debugging Instructions for macOS

Quick setup guide to debug your Numaflow app in IntelliJ while keeping infrastructure in Kubernetes.

## Prerequisites ✅

- Java 21+ (you have Amazon Corretto 23 ✓)
- IntelliJ IDEA 
- Docker Desktop
- kubectl access to your K8s cluster

## Quick Start (5 minutes)

### Step 1: Choose Infrastructure

**Option A: Use Kubernetes Kafka (Recommended)**
```bash
# Set up port forwarding to your K8s cluster
./scripts/setup-k8s-forwarding.sh
```

**Option B: Use Local Kafka**
```bash
# Start local Kafka with Docker
docker-compose -f docker-compose-local.yml up -d

# Verify it's running
docker ps | grep kafka
```

### Step 2: Run in IntelliJ

1. **Open project** in IntelliJ IDEA
2. **Go to**: Run → Edit Configurations
3. **Select**: "Local Development (Debug)" (pre-configured)
4. **Click**: Debug button (🐛) or press `Shift + F9`

You should see:
```
🚀 numaflow-enrichment-app-local started successfully!
🌐 REST API: http://localhost:8080
```

### Step 3: Test & Debug

**Set breakpoints** in your code, then test:

```bash
# Test debug endpoint
curl -X POST http://localhost:8080/api/debug/numaflow/process \
  -H "Content-Type: application/json" \
  -d '{"id":"test-1","title":"Apple Inc announces new iPhone in Cupertino"}'

# Direct enrichment (simpler)
curl -X POST http://localhost:8080/api/debug/direct/enrich \
  -H "Content-Type: application/json" \
  -d '{"id":"test-2","title":"Microsoft partners with OpenAI"}'
```

## Key Debugging Locations

Set breakpoints in these methods:

- `EnrichmentVertex.processMessage()` - Main Numaflow entry point
- `EventEnrichmentService.enrichEvent()` - Business logic  
- `NlpEnrichmentService.enrichText()` - NLP processing
- `DebugController` methods - REST endpoints

## Testing Full Pipeline

```bash
# Generate test data to Kafka
curl -X POST http://localhost:8080/api/test/generate-batch \
  -H "Content-Type: application/json" \
  -d '{"count": 3}'

# Monitor processing
curl http://localhost:8080/actuator/metrics
```

## Troubleshooting

### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Kill if needed
kill -9 <PID>
```

### Can't Connect to Kafka
```bash
# Check port forwarding is running
ps aux | grep "kubectl port-forward"

# Check local Kafka
docker-compose -f docker-compose-local.yml ps

# Test connection
telnet localhost 9092
```

### Application Won't Start
```bash
# Check Java version
java -version  # Should be 21+

# Check profile is set
echo $SPRING_PROFILES_ACTIVE  # Should be 'local'
```

## Cleanup

```bash
# Stop port forwarding
./scripts/cleanup-k8s-forwarding.sh

# Or stop local Kafka
docker-compose -f docker-compose-local.yml down
```

## Manual IntelliJ Setup

If run configuration doesn't exist:

1. **Run** → **Edit Configurations** → **+** → **Application**
2. **Name**: `Local Development Debug`
3. **Main class**: `com.example.numaflow.LocalDevelopmentApplication`
4. **VM options**: `-Xmx1g -Dspring.profiles.active=local`
5. **Use classpath of module**: `numaflow-enrichment-app`
6. **JRE**: Project SDK (23)

## Useful Commands

```bash
# Check what's running
netstat -an | grep -E ":(8080|9092)"

# View logs
tail -f /tmp/numaflow-debug.log

# Check K8s status
kubectl get pods -n numaflow-system

# Test endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/debug/info
```

## Success! 🎉

You can now:
- Set breakpoints and step through code
- Debug the same logic that runs in production
- Test via REST endpoints or full Kafka flow
- Hot reload code changes in IntelliJ

The `/api/debug/numaflow/process` endpoint calls the **exact same** `processMessage()` method that Numaflow uses in production.
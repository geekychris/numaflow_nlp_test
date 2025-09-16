# Numaflow Deployment and Testing Guide

This guide provides complete instructions for deploying and testing the text enrichment application with Numaflow on Kubernetes.

## Prerequisites

Before starting, ensure you have:

1. **Kubernetes cluster** (local or cloud)
2. **kubectl** configured to connect to your cluster  
3. **Docker** for building images
4. **Numaflow installed** on your cluster
5. **Kafka** running in your cluster

## Step 1: Install Numaflow

If Numaflow is not already installed:

```bash
# Create numaflow-system namespace
kubectl create namespace numaflow-system

# Install Numaflow using Helm
helm repo add numaproj https://numaproj.github.io/helm-charts/
helm repo update
helm install numaflow numaproj/numaflow --namespace numaflow-system

# Wait for Numaflow to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=numaflow -n numaflow-system --timeout=300s
```

## Step 2: Install Kafka (if not already available)

```bash
# Install Kafka using Strimzi operator
kubectl create namespace kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

# Wait for Strimzi operator to be ready
kubectl wait --for=condition=ready pod -l name=strimzi-cluster-operator -n kafka --timeout=300s

# Create Kafka cluster
cat <<EOF | kubectl apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: kafka
  namespace: kafka
spec:
  kafka:
    version: 3.5.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      type: jbod
      volumes:
      - id: 0
        type: persistent-claim
        size: 10Gi
        deleteClaim: false
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 5Gi
      deleteClaim: false
  entityOperator:
    topicOperator: {}
    userOperator: {}
EOF

# Wait for Kafka to be ready
kubectl wait kafka/kafka --for=condition=Ready --timeout=300s -n kafka

# Create required topics
cat <<EOF | kubectl apply -f -
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: events
  namespace: kafka
  labels:
    strimzi.io/cluster: kafka
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: enriched-events
  namespace: kafka
  labels:
    strimzi.io/cluster: kafka
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: skipped-events
  namespace: kafka
  labels:
    strimzi.io/cluster: kafka
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: enrichment-errors
  namespace: kafka
  labels:
    strimzi.io/cluster: kafka
spec:
  partitions: 3
  replicas: 3
EOF
```

## Step 3: Build and Push Docker Image

```bash
# Build the Docker image
docker build -t numaflow/text-enrichment-app:latest .

# If using a remote registry, tag and push
# docker tag numaflow/text-enrichment-app:latest your-registry/text-enrichment-app:latest
# docker push your-registry/text-enrichment-app:latest
```

**For local clusters (minikube, kind, etc.):**
```bash
# Load image into local cluster
# For minikube:
minikube image load numaflow/text-enrichment-app:latest

# For kind:
kind load docker-image numaflow/text-enrichment-app:latest
```

## Step 4: Deploy the Numaflow Pipeline

```bash
# Apply the pipeline configuration
kubectl apply -f k8s/numaflow-pipeline.yaml

# Check pipeline status
kubectl get pipeline -n numaflow-system
kubectl describe pipeline text-enrichment-pipeline -n numaflow-system

# Wait for pipeline to be ready
kubectl wait --for=condition=ready pipeline/text-enrichment-pipeline -n numaflow-system --timeout=300s
```

## Step 5: Verify Deployment

```bash
# Check all pipeline vertices
kubectl get vertices -n numaflow-system

# Check pods
kubectl get pods -n numaflow-system -l numaflow.numaproj.io/pipeline-name=text-enrichment-pipeline

# Check logs of the UDF pods
kubectl logs -n numaflow-system -l numaflow.numaproj.io/vertex-name=text-enricher --tail=50

# Check Numaflow controller logs if needed
kubectl logs -n numaflow-system -l app.kubernetes.io/name=numaflow --tail=50
```

## Step 6: Test the Pipeline

### Test 1: Send Test Messages

```bash
# Create a test message producer pod
kubectl run kafka-producer --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- bash

# Inside the producer pod, send test messages:
kafka-console-producer --bootstrap-server kafka.kafka:9092 --topic events

# Send some test JSON messages (one per line):
{"id":"test-1","title":"Apple Inc. announces breakthrough AI technology","description":"The technology giant Apple Inc., based in Cupertino California, announced today major advances in artificial intelligence research.","timestamp":"2025-09-15T20:00:00Z"}

{"id":"test-2","title":"Microsoft partners with Google","description":"Software giants Microsoft and Google announced a strategic partnership. The collaboration focuses on cloud computing and AI development.","timestamp":"2025-09-15T20:01:00Z"}

{"id":"test-3","title":"","description":"","timestamp":"2025-09-15T20:02:00Z"}

# Exit with Ctrl+D
```

### Test 2: Verify Output Topics

```bash
# Check enriched events
kubectl run kafka-consumer --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- \
  kafka-console-consumer --bootstrap-server kafka.kafka:9092 --topic enriched-events --from-beginning

# Check skipped events (in another terminal)
kubectl run kafka-consumer-skipped --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- \
  kafka-console-consumer --bootstrap-server kafka.kafka:9092 --topic skipped-events --from-beginning

# Check error events (in another terminal)
kubectl run kafka-consumer-errors --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- \
  kafka-console-consumer --bootstrap-server kafka.kafka:9092 --topic enrichment-errors --from-beginning
```

### Test 3: Monitor Pipeline Metrics

```bash
# Port forward to access Numaflow UI (if available)
kubectl port-forward -n numaflow-system svc/numaflow-server 8080:8080

# View pipeline metrics
curl -s http://localhost:8080/api/v1/namespaces/numaflow-system/pipelines/text-enrichment-pipeline | jq

# Check vertex metrics
kubectl get vertices -n numaflow-system -o wide
```

## Step 7: Scale Testing

### Load Test with Multiple Messages

```bash
# Create a script to send multiple messages
cat <<EOF > send-test-data.sh
#!/bin/bash

for i in {1..100}; do
  echo "{\"id\":\"test-\$i\",\"title\":\"Test message \$i about technology companies\",\"description\":\"This is test message \$i discussing Apple Inc., Microsoft, Google, and Amazon in various business contexts.\",\"timestamp\":\"2025-09-15T20:0\$((\$i%60)):00Z\"}"
done
EOF

chmod +x send-test-data.sh

# Send the messages
kubectl run kafka-load-test --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- bash -c "
cat <<'SCRIPT' > /tmp/test-data.sh
$(cat send-test-data.sh)
SCRIPT
chmod +x /tmp/test-data.sh
/tmp/test-data.sh | kafka-console-producer --bootstrap-server kafka.kafka:9092 --topic events
"
```

### Monitor Processing

```bash
# Watch pipeline processing
watch kubectl get vertices -n numaflow-system

# Monitor pod resource usage
kubectl top pods -n numaflow-system -l numaflow.numaproj.io/pipeline-name=text-enrichment-pipeline

# Check processing rates
kubectl logs -n numaflow-system -l numaflow.numaproj.io/vertex-name=text-enricher --tail=20 -f
```

## Step 8: Troubleshooting

### Common Issues and Solutions

1. **Pipeline not starting:**
   ```bash
   kubectl describe pipeline text-enrichment-pipeline -n numaflow-system
   kubectl logs -n numaflow-system -l app.kubernetes.io/name=numaflow
   ```

2. **UDF pods crashing:**
   ```bash
   kubectl describe pods -n numaflow-system -l numaflow.numaproj.io/vertex-name=text-enricher
   kubectl logs -n numaflow-system -l numaflow.numaproj.io/vertex-name=text-enricher --previous
   ```

3. **Messages not being processed:**
   ```bash
   # Check Kafka connectivity
   kubectl run kafka-test --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- \
     kafka-topics --bootstrap-server kafka.kafka:9092 --list
   
   # Check if messages are in input topic
   kubectl run kafka-check --image=confluentinc/cp-kafka:latest --rm -it --restart=Never -- \
     kafka-console-consumer --bootstrap-server kafka.kafka:9092 --topic events --from-beginning --timeout-ms 10000
   ```

4. **Performance issues:**
   ```bash
   # Scale up the UDF
   kubectl patch pipeline text-enrichment-pipeline -n numaflow-system --type='merge' -p='{"spec":{"vertices":[{"name":"text-enricher","scale":{"min":3,"max":10}}]}}'
   
   # Monitor resource usage
   kubectl top pods -n numaflow-system
   ```

## Step 9: Cleanup

```bash
# Delete the pipeline
kubectl delete -f k8s/numaflow-pipeline.yaml

# Delete test pods (if any are stuck)
kubectl delete pod kafka-producer kafka-consumer kafka-consumer-skipped kafka-consumer-errors kafka-load-test kafka-test kafka-check --ignore-not-found

# Optional: Remove Kafka topics
kubectl delete kafkatopic events enriched-events skipped-events enrichment-errors -n kafka

# Optional: Remove Kafka cluster
kubectl delete kafka kafka -n kafka

# Optional: Uninstall Numaflow
helm uninstall numaflow -n numaflow-system
```

## Expected Results

When everything is working correctly, you should see:

1. **Input messages** sent to `events` topic
2. **Enriched messages** with NLP processing in `enriched-events` topic
3. **Empty/invalid messages** in `skipped-events` topic  
4. **Error messages** (if any processing failures) in `enrichment-errors` topic

The enriched messages will contain:
- Original event data
- Text segmentation (sentences)
- Named entity recognition results
- Processing metadata
- Timestamps

This demonstrates a complete Numaflow pipeline with proper message routing based on processing results.

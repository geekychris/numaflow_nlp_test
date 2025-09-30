#!/bin/bash

# Port forwarding setup script for hybrid Numaflow development
# This script forwards Kubernetes services to local ports for development

set -e

echo "🔗 Setting up Kubernetes port forwarding for hybrid development..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}❌ kubectl is not installed or not in PATH${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ kubectl found${NC}"
}

# Function to check if cluster is reachable
check_cluster() {
    echo -e "${BLUE}🔍 Checking Kubernetes cluster connectivity...${NC}"
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}❌ Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Connected to cluster: $(kubectl config current-context)${NC}"
}

# Function to setup Kafka port forwarding
setup_kafka_forwarding() {
    echo -e "${BLUE}🎯 Setting up Kafka port forwarding...${NC}"
    
    # Check if Kafka service exists
    if kubectl get svc kafka -n kafka &> /dev/null; then
        echo -e "${YELLOW}📡 Forwarding Kafka: localhost:9092 -> kafka:9092${NC}"
        kubectl port-forward svc/kafka 9092:9092 -n kafka &
        KAFKA_PID=$!
        echo "Kafka port-forward PID: $KAFKA_PID"
        sleep 2
    elif kubectl get svc kafka -n numaflow-system &> /dev/null; then
        echo -e "${YELLOW}📡 Forwarding Kafka: localhost:9092 -> kafka:9092${NC}"
        kubectl port-forward svc/kafka 9092:9092 -n numaflow-system &
        KAFKA_PID=$!
        echo "Kafka port-forward PID: $KAFKA_PID"
        sleep 2
    else
        echo -e "${YELLOW}⚠️  Kafka service not found in 'kafka' or 'numaflow-system' namespaces${NC}"
        echo -e "${YELLOW}   You might need to adjust the namespace or use local Kafka${NC}"
    fi
}

# Function to setup other services port forwarding
setup_other_forwarding() {
    echo -e "${BLUE}🎯 Setting up other service port forwarding...${NC}"
    
    # Numaflow Dashboard (if exists)
    if kubectl get svc numaflow-server -n numaflow-system &> /dev/null; then
        echo -e "${YELLOW}📊 Forwarding Numaflow Dashboard: localhost:8443 -> numaflow-server:8443${NC}"
        kubectl port-forward svc/numaflow-server 8443:8443 -n numaflow-system &
        NUMAFLOW_PID=$!
        echo "Numaflow Dashboard port-forward PID: $NUMAFLOW_PID"
        sleep 1
    fi
    
    # Any existing enrichment service (for monitoring)
    if kubectl get svc text-enrichment-service -n numaflow-system &> /dev/null; then
        echo -e "${YELLOW}📈 Forwarding Enrichment Service: localhost:8081 -> text-enrichment-service:80${NC}"
        kubectl port-forward svc/text-enrichment-service 8081:80 -n numaflow-system &
        ENRICHMENT_PID=$!
        echo "Enrichment Service port-forward PID: $ENRICHMENT_PID"
        sleep 1
    fi
}

# Function to save PIDs for cleanup
save_pids() {
    PID_FILE="$HOME/.numaflow-port-forward-pids"
    echo "# Port forward PIDs for Numaflow development" > "$PID_FILE"
    echo "# Generated on $(date)" >> "$PID_FILE"
    
    if [[ -n "$KAFKA_PID" ]]; then
        echo "KAFKA_PID=$KAFKA_PID" >> "$PID_FILE"
    fi
    if [[ -n "$NUMAFLOW_PID" ]]; then
        echo "NUMAFLOW_PID=$NUMAFLOW_PID" >> "$PID_FILE"
    fi
    if [[ -n "$ENRICHMENT_PID" ]]; then
        echo "ENRICHMENT_PID=$ENRICHMENT_PID" >> "$PID_FILE"
    fi
    
    echo -e "${GREEN}💾 PIDs saved to $PID_FILE${NC}"
}

# Function to display connection info
show_connection_info() {
    echo -e "\n${GREEN}🎉 Port forwarding setup complete!${NC}"
    echo -e "\n${BLUE}📋 Connection Information:${NC}"
    echo -e "   ${YELLOW}Local Kafka:${NC} localhost:9092"
    echo -e "   ${YELLOW}Numaflow Dashboard:${NC} http://localhost:8443 (if available)"
    echo -e "   ${YELLOW}Enrichment Monitoring:${NC} http://localhost:8081 (if available)"
    echo -e "\n${BLUE}🧪 Your local app should connect to:${NC}"
    echo -e "   ${YELLOW}Kafka Bootstrap Servers:${NC} localhost:9092"
    echo -e "\n${BLUE}🔧 To stop port forwarding:${NC}"
    echo -e "   Run: ${YELLOW}./scripts/cleanup-k8s-forwarding.sh${NC}"
    echo -e "\n${BLUE}📝 Monitor connections:${NC}"
    echo -e "   ${YELLOW}netstat -an | grep :9092${NC} (check Kafka)"
    echo -e "   ${YELLOW}kubectl get pods -n numaflow-system${NC} (check K8s status)"
}

# Main execution
main() {
    echo -e "${BLUE}🚀 Numaflow Hybrid Development Setup${NC}"
    echo -e "${BLUE}======================================${NC}"
    
    check_kubectl
    check_cluster
    setup_kafka_forwarding
    setup_other_forwarding
    save_pids
    show_connection_info
    
    echo -e "\n${GREEN}✨ Setup complete! Port forwarding is running in the background.${NC}"
    echo -e "${YELLOW}💡 Tip: Keep this terminal session open to maintain connections.${NC}"
}

# Run main function
main "$@"
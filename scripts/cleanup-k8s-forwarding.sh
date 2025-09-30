#!/bin/bash

# Cleanup script for Kubernetes port forwarding
# This script stops all port forwarding processes started by setup-k8s-forwarding.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🧹 Cleaning up Kubernetes port forwarding...${NC}"

PID_FILE="$HOME/.numaflow-port-forward-pids"

# Function to kill a process by PID
kill_process() {
    local pid=$1
    local name=$2
    
    if [[ -n "$pid" ]]; then
        if kill -0 "$pid" 2>/dev/null; then
            echo -e "${YELLOW}🔪 Killing $name (PID: $pid)${NC}"
            kill "$pid"
            sleep 1
            if kill -0 "$pid" 2>/dev/null; then
                echo -e "${RED}⚠️  Force killing $name (PID: $pid)${NC}"
                kill -9 "$pid" 2>/dev/null || true
            fi
            echo -e "${GREEN}✅ $name stopped${NC}"
        else
            echo -e "${YELLOW}⚠️  $name (PID: $pid) was already stopped${NC}"
        fi
    fi
}

# Function to cleanup using saved PIDs
cleanup_saved_pids() {
    if [[ -f "$PID_FILE" ]]; then
        echo -e "${BLUE}📁 Reading PIDs from $PID_FILE${NC}"
        
        # Source the PID file to get variables
        source "$PID_FILE"
        
        kill_process "$KAFKA_PID" "Kafka port-forward"
        kill_process "$NUMAFLOW_PID" "Numaflow Dashboard port-forward"
        kill_process "$ENRICHMENT_PID" "Enrichment Service port-forward"
        
        # Remove the PID file
        rm -f "$PID_FILE"
        echo -e "${GREEN}🗑️  Removed $PID_FILE${NC}"
    else
        echo -e "${YELLOW}📁 No PID file found at $PID_FILE${NC}"
    fi
}

# Function to cleanup any remaining kubectl port-forward processes
cleanup_remaining_processes() {
    echo -e "${BLUE}🔍 Looking for remaining kubectl port-forward processes...${NC}"
    
    # Find all kubectl port-forward processes
    KUBECTL_PIDS=$(pgrep -f "kubectl.*port-forward" || true)
    
    if [[ -n "$KUBECTL_PIDS" ]]; then
        echo -e "${YELLOW}🔍 Found kubectl port-forward processes: $KUBECTL_PIDS${NC}"
        for pid in $KUBECTL_PIDS; do
            # Get process details
            PROCESS_INFO=$(ps -p "$pid" -o command= 2>/dev/null || echo "Unknown process")
            echo -e "${YELLOW}🔪 Killing: $PROCESS_INFO${NC}"
            kill "$pid" 2>/dev/null || true
        done
        sleep 2
        echo -e "${GREEN}✅ Remaining kubectl processes cleaned up${NC}"
    else
        echo -e "${GREEN}✅ No remaining kubectl port-forward processes found${NC}"
    fi
}

# Function to verify cleanup
verify_cleanup() {
    echo -e "${BLUE}🔍 Verifying cleanup...${NC}"
    
    # Check if any processes are still listening on the forwarded ports
    local ports=("9092" "8443" "8081")
    local any_active=false
    
    for port in "${ports[@]}"; do
        if lsof -i ":$port" &>/dev/null; then
            echo -e "${YELLOW}⚠️  Port $port is still in use${NC}"
            lsof -i ":$port" | head -2
            any_active=true
        fi
    done
    
    if [[ "$any_active" == "false" ]]; then
        echo -e "${GREEN}✅ All ports are free${NC}"
    fi
}

# Function to show status
show_status() {
    echo -e "\n${GREEN}🎉 Cleanup complete!${NC}"
    echo -e "\n${BLUE}📋 Status:${NC}"
    echo -e "   ${GREEN}✅ Port forwarding processes stopped${NC}"
    echo -e "   ${GREEN}✅ PID file removed${NC}"
    echo -e "\n${BLUE}🔧 Next steps:${NC}"
    echo -e "   ${YELLOW}• You can restart port forwarding with: ./scripts/setup-k8s-forwarding.sh${NC}"
    echo -e "   ${YELLOW}• Or use local Kafka with: docker-compose -f docker-compose-local.yml up -d${NC}"
}

# Main execution
main() {
    echo -e "${BLUE}🚀 Numaflow Port Forwarding Cleanup${NC}"
    echo -e "${BLUE}====================================${NC}"
    
    cleanup_saved_pids
    cleanup_remaining_processes
    verify_cleanup
    show_status
    
    echo -e "\n${GREEN}✨ All clean!${NC}"
}

# Run main function
main "$@"
#!/bin/bash
set -e

# Test script for Numaflow Text Enrichment Application

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Test configuration
RUN_UNIT_TESTS=true
RUN_INTEGRATION_TESTS=true
GENERATE_COVERAGE=false
VERBOSE=false
CLEAN=false

# Usage function
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -c, --clean             Clean before testing"
    echo "  -u, --unit-only         Run only unit tests"
    echo "  -i, --integration-only  Run only integration tests"
    echo "  -g, --coverage          Generate test coverage report"
    echo "  -v, --verbose           Verbose output"
    echo ""
    echo "Examples:"
    echo "  $0                      # Run all tests"
    echo "  $0 --unit-only          # Run only unit tests"
    echo "  $0 --coverage           # Run tests with coverage"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -u|--unit-only)
            RUN_INTEGRATION_TESTS=false
            shift
            ;;
        -i|--integration-only)
            RUN_UNIT_TESTS=false
            shift
            ;;
        -g|--coverage)
            GENERATE_COVERAGE=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}       Test Runner${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Change to project directory
cd "$PROJECT_DIR"

# Print test configuration
echo -e "${YELLOW}Test Configuration:${NC}"
echo "  Project Directory: $PROJECT_DIR"
echo "  Clean Build: $CLEAN"
echo "  Unit Tests: $RUN_UNIT_TESTS"
echo "  Integration Tests: $RUN_INTEGRATION_TESTS"
echo "  Generate Coverage: $GENERATE_COVERAGE"
echo "  Verbose: $VERBOSE"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ $JAVA_VERSION -lt 21 ]]; then
    echo -e "${RED}Error: Java 21 or higher is required (found: $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed or not in PATH${NC}"
    exit 1
fi

MVN_VERSION=$(mvn -version 2>&1 | head -n1 | cut -d' ' -f3)
echo -e "${GREEN}✓ Maven $MVN_VERSION${NC}"
echo ""

# Clean if requested
if [[ "$CLEAN" == true ]]; then
    echo -e "${YELLOW}Cleaning project...${NC}"
    mvn clean
    echo -e "${GREEN}✓ Project cleaned${NC}"
    echo ""
fi

# Prepare Maven arguments
MAVEN_ARGS=""
if [[ "$VERBOSE" == true ]]; then
    MAVEN_ARGS="$MAVEN_ARGS -X"
fi

# Test execution tracking
TESTS_PASSED=0
TESTS_FAILED=0
TEST_RESULTS=()

# Function to run tests and track results
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    echo -e "${YELLOW}Running $test_name...${NC}"
    echo "Command: $test_command"
    echo ""
    
    if eval "$test_command"; then
        echo -e "${GREEN}✓ $test_name passed${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        TEST_RESULTS+=("✓ $test_name: PASSED")
    else
        echo -e "${RED}✗ $test_name failed${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        TEST_RESULTS+=("✗ $test_name: FAILED")
        return 1
    fi
    echo ""
}

# Run unit tests
if [[ "$RUN_UNIT_TESTS" == true ]]; then
    TEST_COMMAND="mvn $MAVEN_ARGS test -Dtest='!*IntegrationTest'"
    
    if ! run_test "Unit Tests" "$TEST_COMMAND"; then
        echo -e "${RED}Unit tests failed, stopping execution${NC}"
        exit 1
    fi
fi

# Run integration tests
if [[ "$RUN_INTEGRATION_TESTS" == true ]]; then
    TEST_COMMAND="mvn $MAVEN_ARGS test -Dtest='*IntegrationTest'"
    
    if ! run_test "Integration Tests" "$TEST_COMMAND"; then
        echo -e "${YELLOW}Integration tests failed, but continuing...${NC}"
    fi
fi

# Generate test coverage if requested
if [[ "$GENERATE_COVERAGE" == true ]]; then
    echo -e "${YELLOW}Generating test coverage report...${NC}"
    
    # Run tests with JaCoCo
    if mvn $MAVEN_ARGS clean test jacoco:report; then
        echo -e "${GREEN}✓ Coverage report generated${NC}"
        
        # Find and display coverage report location
        COVERAGE_REPORT="$PROJECT_DIR/target/site/jacoco/index.html"
        if [[ -f "$COVERAGE_REPORT" ]]; then
            echo -e "${GREEN}Coverage report available at: $COVERAGE_REPORT${NC}"
            
            # Extract coverage summary if possible
            if command -v xmllint &> /dev/null && [[ -f "$PROJECT_DIR/target/site/jacoco/jacoco.xml" ]]; then
                COVERAGE_XML="$PROJECT_DIR/target/site/jacoco/jacoco.xml"
                INSTRUCTION_COVERED=$(xmllint --xpath "string(//counter[@type='INSTRUCTION']/@covered)" "$COVERAGE_XML" 2>/dev/null || echo "0")
                INSTRUCTION_MISSED=$(xmllint --xpath "string(//counter[@type='INSTRUCTION']/@missed)" "$COVERAGE_XML" 2>/dev/null || echo "0")
                
                if [[ "$INSTRUCTION_COVERED" != "0" && "$INSTRUCTION_MISSED" != "0" ]]; then
                    TOTAL_INSTRUCTIONS=$((INSTRUCTION_COVERED + INSTRUCTION_MISSED))
                    COVERAGE_PERCENT=$(( (INSTRUCTION_COVERED * 100) / TOTAL_INSTRUCTIONS ))
                    echo -e "${GREEN}Code Coverage: ${COVERAGE_PERCENT}%${NC}"
                fi
            fi
        fi
    else
        echo -e "${RED}✗ Coverage report generation failed${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        TEST_RESULTS+=("✗ Coverage Report: FAILED")
    fi
    echo ""
fi

# Create test data for manual testing
echo -e "${YELLOW}Creating test data...${NC}"
TEST_DATA_DIR="$PROJECT_DIR/test-data"
mkdir -p "$TEST_DATA_DIR"

# Sample events for testing
cat > "$TEST_DATA_DIR/sample-events.json" << 'EOF'
{"id": "event-001", "title": "Apple Inc. reports strong quarterly results", "description": "The technology giant Apple Inc., based in Cupertino California, announced today that its quarterly revenue exceeded expectations. CEO Tim Cook praised the team's efforts.", "timestamp": "2024-01-15T10:00:00Z"}
{"id": "event-002", "title": "Microsoft launches new AI initiative", "description": "Microsoft Corporation unveiled its latest artificial intelligence research program in Seattle, Washington. The initiative will be led by researcher Dr. Sarah Johnson.", "timestamp": "2024-01-15T11:00:00Z"}  
{"id": "event-003", "title": "Google expands cloud services", "description": "Google announced the expansion of its cloud computing services to new regions including Tokyo, Japan and London, England.", "timestamp": "2024-01-15T12:00:00Z"}
EOF

echo -e "${GREEN}✓ Test data created at $TEST_DATA_DIR/sample-events.json${NC}"
echo ""

# Performance test (basic)
echo -e "${YELLOW}Running performance test...${NC}"
if mvn $MAVEN_ARGS test -Dtest="*IntegrationTest" -Dspring.profiles.active=test > /tmp/perf-test.log 2>&1; then
    # Extract timing information from logs if available
    if grep -q "processing.*ms" /tmp/perf-test.log; then
        echo -e "${GREEN}✓ Performance test completed${NC}"
        echo "Performance metrics:"
        grep "processing.*ms" /tmp/perf-test.log | tail -3
    else
        echo -e "${GREEN}✓ Performance test completed (no timing data available)${NC}"
    fi
else
    echo -e "${YELLOW}Performance test completed with warnings${NC}"
fi
echo ""

# Test summary
echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}           Test Summary${NC}"
echo -e "${BLUE}===========================================${NC}"

if [[ ${#TEST_RESULTS[@]} -gt 0 ]]; then
    for result in "${TEST_RESULTS[@]}"; do
        echo "  $result"
    done
    echo ""
fi

echo "Test Statistics:"
echo "  Total Passed: $TESTS_PASSED"
echo "  Total Failed: $TESTS_FAILED"
echo "  Total Tests: $((TESTS_PASSED + TESTS_FAILED))"

if [[ $TESTS_FAILED -eq 0 ]]; then
    echo -e "${GREEN}✓ All tests passed successfully!${NC}"
    EXIT_CODE=0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    EXIT_CODE=1
fi

echo ""
echo "Next steps:"
echo "  • Build application: ./scripts/build.sh"
echo "  • Test with Docker: docker-compose up -d"
echo "  • Deploy to Kubernetes: kubectl apply -f k8s/"

if [[ "$GENERATE_COVERAGE" == true && -f "$PROJECT_DIR/target/site/jacoco/index.html" ]]; then
    echo "  • View coverage report: open target/site/jacoco/index.html"
fi

echo ""
exit $EXIT_CODE

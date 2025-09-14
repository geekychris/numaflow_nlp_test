#!/bin/bash
set -e

# Build script for Numaflow Text Enrichment Application

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default values
SKIP_TESTS=false
BUILD_DOCKER=false
PUSH_DOCKER=false
DOCKER_REGISTRY=""
DOCKER_TAG="latest"
CLEAN=false
DOWNLOAD_MODELS=false

# Usage function
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help           Show this help message"
    echo "  -c, --clean          Clean build (mvn clean)"
    echo "  -t, --skip-tests     Skip running tests"
    echo "  -d, --docker         Build Docker image"
    echo "  -p, --push           Push Docker image to registry"
    echo "  -r, --registry       Docker registry (required with --push)"
    echo "  -T, --tag            Docker tag (default: latest)"
    echo "  -m, --models         Download NLP models"
    echo ""
    echo "Examples:"
    echo "  $0                          # Basic build"
    echo "  $0 --clean --skip-tests     # Clean build without tests"
    echo "  $0 --docker --tag v1.0.0    # Build with Docker image"
    echo "  $0 --push --registry my-registry.com --tag v1.0.0"
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
        -t|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -d|--docker)
            BUILD_DOCKER=true
            shift
            ;;
        -p|--push)
            PUSH_DOCKER=true
            shift
            ;;
        -r|--registry)
            DOCKER_REGISTRY="$2"
            shift 2
            ;;
        -T|--tag)
            DOCKER_TAG="$2"
            shift 2
            ;;
        -m|--models)
            DOWNLOAD_MODELS=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Validate arguments
if [[ "$PUSH_DOCKER" == true && -z "$DOCKER_REGISTRY" ]]; then
    echo -e "${RED}Error: --registry is required when using --push${NC}"
    exit 1
fi

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}  Numaflow Text Enrichment App Builder${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Change to project directory
cd "$PROJECT_DIR"

# Print build configuration
echo -e "${YELLOW}Build Configuration:${NC}"
echo "  Project Directory: $PROJECT_DIR"
echo "  Clean Build: $CLEAN"
echo "  Skip Tests: $SKIP_TESTS"
echo "  Build Docker: $BUILD_DOCKER"
echo "  Push Docker: $PUSH_DOCKER"
echo "  Docker Registry: ${DOCKER_REGISTRY:-'N/A'}"
echo "  Docker Tag: $DOCKER_TAG"
echo "  Download Models: $DOWNLOAD_MODELS"
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

# Check Docker (if needed)
if [[ "$BUILD_DOCKER" == true ]]; then
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: Docker is not installed or not in PATH${NC}"
        exit 1
    fi
    DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | sed 's/,//')
    echo -e "${GREEN}✓ Docker $DOCKER_VERSION${NC}"
fi

echo ""

# Download NLP models if requested
if [[ "$DOWNLOAD_MODELS" == true ]]; then
    echo -e "${YELLOW}Downloading NLP models...${NC}"
    if [[ -f "$PROJECT_DIR/scripts/download-models.sh" ]]; then
        chmod +x "$PROJECT_DIR/scripts/download-models.sh"
        # Modify script to download to local models directory
        MODEL_DIR="$PROJECT_DIR/models"
        mkdir -p "$MODEL_DIR"
        
        # Run download script with local directory
        sed "s|/app/models|$MODEL_DIR|g" "$PROJECT_DIR/scripts/download-models.sh" | bash
    else
        echo -e "${YELLOW}Warning: download-models.sh not found, skipping model download${NC}"
    fi
    echo ""
fi

# Maven build
echo -e "${YELLOW}Building application...${NC}"

MAVEN_ARGS=""
if [[ "$CLEAN" == true ]]; then
    MAVEN_ARGS="clean"
fi

if [[ "$SKIP_TESTS" == true ]]; then
    MAVEN_ARGS="$MAVEN_ARGS package -DskipTests"
else
    MAVEN_ARGS="$MAVEN_ARGS package"
fi

echo "Running: mvn $MAVEN_ARGS"
if ! mvn $MAVEN_ARGS; then
    echo -e "${RED}Error: Maven build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Maven build completed${NC}"
echo ""

# Find the built JAR file
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -n1)
if [[ -z "$JAR_FILE" ]]; then
    echo -e "${RED}Error: Could not find built JAR file${NC}"
    exit 1
fi

echo -e "${GREEN}Built JAR: $JAR_FILE${NC}"
JAR_SIZE=$(stat -f%z "$JAR_FILE" 2>/dev/null || stat -c%s "$JAR_FILE" 2>/dev/null || echo "unknown")
if [[ "$JAR_SIZE" != "unknown" ]]; then
    JAR_SIZE_MB=$((JAR_SIZE / 1024 / 1024))
    echo -e "${GREEN}JAR size: ${JAR_SIZE_MB}MB${NC}"
fi

# Docker build
if [[ "$BUILD_DOCKER" == true ]]; then
    echo -e "${YELLOW}Building Docker image...${NC}"
    
    IMAGE_NAME="numaflow/text-enrichment-app"
    FULL_IMAGE_NAME="$IMAGE_NAME:$DOCKER_TAG"
    
    if [[ -n "$DOCKER_REGISTRY" ]]; then
        FULL_IMAGE_NAME="$DOCKER_REGISTRY/$IMAGE_NAME:$DOCKER_TAG"
    fi
    
    echo "Building: $FULL_IMAGE_NAME"
    
    if ! docker build -t "$FULL_IMAGE_NAME" .; then
        echo -e "${RED}Error: Docker build failed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker image built: $FULL_IMAGE_NAME${NC}"
    
    # Show image size
    IMAGE_SIZE=$(docker images "$FULL_IMAGE_NAME" --format "{{.Size}}")
    echo -e "${GREEN}Docker image size: $IMAGE_SIZE${NC}"
    
    # Push Docker image
    if [[ "$PUSH_DOCKER" == true ]]; then
        echo -e "${YELLOW}Pushing Docker image...${NC}"
        
        if ! docker push "$FULL_IMAGE_NAME"; then
            echo -e "${RED}Error: Docker push failed${NC}"
            exit 1
        fi
        
        echo -e "${GREEN}✓ Docker image pushed: $FULL_IMAGE_NAME${NC}"
    fi
fi

# Build summary
echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}           Build Summary${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}✓ Build completed successfully${NC}"
echo ""
echo "Artifacts:"
echo "  JAR file: $JAR_FILE"

if [[ "$BUILD_DOCKER" == true ]]; then
    echo "  Docker image: $FULL_IMAGE_NAME"
fi

if [[ "$DOWNLOAD_MODELS" == true ]]; then
    echo "  NLP models: $PROJECT_DIR/models/"
fi

echo ""
echo "Next steps:"
echo "  • Run locally: java -jar $JAR_FILE"
echo "  • Run with Docker: docker run -p 8080:8080 $FULL_IMAGE_NAME"
echo "  • Deploy to Kubernetes: kubectl apply -f k8s/"
echo "  • Test with: docker-compose up -d"
echo ""

exit 0

#!/bin/bash

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

log_success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

log_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

# Main banner
cat <<EOF
======================================================
     WVP-GB28181-PRO Execution Script
======================================================
EOF

# Get current script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Dependency checks
log_info "Checking system dependencies..."

has_java=false
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    log_success "Java is installed: $java_version"
    has_java=true
else
    log_warning "Java is NOT installed. You need Java 21 to run the backend locally."
fi

has_node=false
if command -v node &> /dev/null; then
    node_version=$(node -v)
    log_success "Node.js is installed: $node_version"
    has_node=true
else
    log_warning "Node.js is NOT installed. You need Node.js to compile the Web frontend."
fi

has_mvn=false
if command -v mvn &> /dev/null; then
    mvn_version=$(mvn -v | head -n 1)
    log_success "Maven is installed: $mvn_version"
    has_mvn=true
else
    log_warning "Maven is NOT installed. You cannot compile the Java backend locally."
fi

has_docker=false
if command -v docker &> /dev/null; then
    docker_version=$(docker -v)
    log_success "Docker is installed: $docker_version"
    has_docker=true
else
    log_info "Docker is NOT installed. Docker Compose mode will not be available."
fi

# Ask the user how they want to run the project
echo ""
echo "Select execution mode:"
echo "1) Run with Docker Compose (Recommended - builds and starts all services: WVP, Web, Redis, MySQL, ZLMediaKit, Nginx)"
echo "2) Run locally (Requires local Redis, MySQL, ZLMediaKit already running on localhost)"
read -p "Enter selection (1 or 2): " mode_choice

if [ "$mode_choice" = "1" ]; then
    if [ "$has_docker" = "false" ]; then
        log_error "Docker is required for this mode but it is not installed. Exiting."
        exit 1
    fi
    
    log_info "Starting full stack with Docker Compose..."
    cd "$SCRIPT_DIR/docker"
    
    # Check if docker-compose or docker compose is available
    if docker compose version &> /dev/null; then
        log_info "Running: docker compose up -d --build"
        docker compose up -d --build
    else
        log_info "Running: docker-compose up -d --build"
        docker-compose up -d --build
    fi
    
    if [ $? -eq 0 ]; then
        log_success "All services started successfully in Docker!"
        log_info "You can access the WVP Web Platform at: http://localhost:8080"
        log_info "Default login credentials: admin / admin"
    else
        log_error "Failed to start Docker Compose services."
        exit 1
    fi

elif [ "$mode_choice" = "2" ]; then
    # Local build and run
    if [ "$has_node" = "false" ]; then
        log_error "Node.js is required to build the frontend. Exiting."
        exit 1
    fi
    
    log_info "Step 1: Building Web Frontend..."
    cd "$SCRIPT_DIR/web"
    
    if command -v yarn &> /dev/null; then
        log_info "Installing dependencies with Yarn (fast)..."
        yarn config set registry https://registry.npmmirror.com
        yarn install --network-timeout 600000 --ignore-engines
        if [ $? -ne 0 ]; then
            log_error "Yarn install failed. Retrying..."
            yarn install --ignore-engines
            if [ $? -ne 0 ]; then
                log_error "Frontend dependency installation failed. Exiting."
                exit 1
            fi
        fi
        log_info "Compiling Web assets with Yarn..."
        yarn build:prod
    else
        log_info "Installing npm dependencies (this may take a few minutes)..."
        npm install --registry=https://registry.npmmirror.com
        if [ $? -ne 0 ]; then
            log_error "npm install failed. Retrying standard npm..."
            npm install
            if [ $? -ne 0 ]; then
                log_error "Frontend dependency installation failed. Exiting."
                exit 1
            fi
        fi
        log_info "Compiling Web assets with npm..."
        npm run build:prod
    fi

    if [ $? -ne 0 ]; then
        log_error "Frontend compilation failed. Exiting."
        exit 1
    fi
    log_success "Web Frontend built successfully! Compiled assets written to backend resource static folder."
    
    cd "$SCRIPT_DIR"
    log_info "Step 2: Starting Backend..."
    
    if [ "$has_mvn" = "true" ]; then
        log_info "Compiling backend with Maven..."
        mvn clean package -Dmaven.test.skip=true
        if [ $? -ne 0 ]; then
            log_error "Backend compilation failed. Exiting."
            exit 1
        fi
        
        log_info "Running compiled jar..."
        java -jar target/wvp-pro-*.jar
    else
        # Try to find prebuilt jar
        prebuilt_jar=$(find target -name "wvp-pro-*.jar" 2>/dev/null | head -n 1)
        if [ -n "$prebuilt_jar" ]; then
            log_info "Found prebuilt jar: $prebuilt_jar. Running it..."
            java -jar "$prebuilt_jar"
        else
            log_error "Maven is not installed and no prebuilt jar was found in 'target/'. Please install Maven (or Java 21) or run WVP via Docker Compose."
            exit 1
        fi
    fi
else
    log_error "Invalid selection. Exiting."
    exit 1
fi

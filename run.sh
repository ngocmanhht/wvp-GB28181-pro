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

# Load environment configuration if present
if [ -f "$SCRIPT_DIR/docker/.env" ]; then
    set -a
    source "$SCRIPT_DIR/docker/.env"
    set +a
fi

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
echo "1) Run WVP Master ONLY (Recommended - Pure Control Plane, KHÔNG kèm ZLMediaKit)"
echo "2) Run All-in-One Full Stack (WVP + ZLMediaKit chạy chung 1 máy để test nhanh)"
echo "3) Run locally (Requires local Redis, MySQL running on localhost)"
read -p "Enter selection (1, 2, or 3): " mode_choice

if [ "$mode_choice" = "1" ]; then
    if [ "$has_docker" = "false" ]; then
        log_error "Docker is required for this mode but it is not installed. Exiting."
        exit 1
    fi
    
    log_info "Preparing storage volumes and checking permissions..."
    mkdir -p "$SCRIPT_DIR/docker/volumes/redis/data" \
             "$SCRIPT_DIR/docker/volumes/mysql/data" \
             "$SCRIPT_DIR/docker/logs/wvp" \
             "$SCRIPT_DIR/docker/logs/mysql" \
             "$SCRIPT_DIR/docker/logs/nginx" \
             "$SCRIPT_DIR/docker/media/record" \
             "$SCRIPT_DIR/docker/logs/media" 2>/dev/null || true
    chmod -R 777 "$SCRIPT_DIR/docker/volumes" "$SCRIPT_DIR/docker/logs" 2>/dev/null || true

    # Clean stale/incompatible Redis AOF cache files (e.g. from previous redis:latest run)
    if [ -d "$SCRIPT_DIR/docker/volumes/redis/data/appendonlydir" ] || [ -f "$SCRIPT_DIR/docker/volumes/redis/data/dump.rdb" ]; then
        log_info "Cleaning stale Redis cache files to ensure clean database startup..."
        rm -rf "$SCRIPT_DIR/docker/volumes/redis/data" && mkdir -p "$SCRIPT_DIR/docker/volumes/redis/data" && chmod -R 777 "$SCRIPT_DIR/docker/volumes/redis/data" 2>/dev/null || true
    fi

    log_info "Starting WVP Master Pure Control Plane (Không kèm ZLMediaKit)..."
    cd "$SCRIPT_DIR/docker"
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    
    # Clean up any previous stopped/unhealthy containers
    if docker compose version &> /dev/null; then
        docker compose -f docker-compose-master.yml down --remove-orphans 2>/dev/null || true
    else
        docker-compose -f docker-compose-master.yml down --remove-orphans 2>/dev/null || true
    fi

    # Check if MySQL data directory was corrupted by a previous interrupted initialization
    if [ -d "$SCRIPT_DIR/docker/volumes/mysql/data" ] && [ "$(ls -A "$SCRIPT_DIR/docker/volumes/mysql/data" 2>/dev/null)" ]; then
        if [ ! -d "$SCRIPT_DIR/docker/volumes/mysql/data/wvp" ] || [ -z "$(ls -A "$SCRIPT_DIR/docker/volumes/mysql/data/wvp" 2>/dev/null)" ]; then
            log_warning "Detected incomplete/corrupted MySQL initialization (database 'wvp' is empty or missing)."
            log_info "Auto-cleaning broken MySQL data files to allow a fresh, healthy initialization..."
            rm -rf "$SCRIPT_DIR/docker/volumes/mysql/data" && mkdir -p "$SCRIPT_DIR/docker/volumes/mysql/data" && chmod -R 777 "$SCRIPT_DIR/docker/volumes/mysql/data" 2>/dev/null || true
        fi
    fi

    log_info "Running: docker compose -f docker-compose-master.yml up -d --build --force-recreate"
    if docker compose version &> /dev/null; then
        docker compose -f docker-compose-master.yml up -d --build --force-recreate
    else
        docker-compose -f docker-compose-master.yml up -d --build --force-recreate
    fi
    
    if [ $? -eq 0 ]; then
        echo ""
        log_info "Synchronizing MySQL user permissions and database schema..."
        compose_cmd="docker compose -f docker-compose-master.yml"
        if ! docker compose version &>/dev/null; then
            compose_cmd="docker-compose -f docker-compose-master.yml"
        fi

        # Wait up to 30s for MySQL to be ready
        log_info "Waiting for MySQL service to accept connections..."
        for attempt in {1..15}; do
            if $compose_cmd exec -T polaris-mysql mysqladmin ping -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" --silent &>/dev/null; then
                break
            fi
            sleep 2
        done

        # Ensure wvp_user exists and has correct password in MySQL
        $compose_cmd exec -T polaris-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" -e "
            CREATE DATABASE IF NOT EXISTS wvp CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
            CREATE USER IF NOT EXISTS '${MYSQL_USER:-wvp_user}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD:-wvp_password}';
            ALTER USER '${MYSQL_USER:-wvp_user}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD:-wvp_password}';
            GRANT ALL PRIVILEGES ON wvp.* TO '${MYSQL_USER:-wvp_user}'@'%';
            FLUSH PRIVILEGES;
        " 2>/dev/null || true

        # Check if tables exist, if not import init-mysql.sql
        table_count=$($compose_cmd exec -T polaris-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'wvp';" 2>/dev/null || echo 0)
        if [ -z "$table_count" ] || [ "$table_count" -lt 5 ]; then
            log_info "Tables not found in 'wvp' database. Importing initial schema..."
            $compose_cmd exec -T polaris-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" wvp < "$SCRIPT_DIR/database/2.7.4/init-mysql-2.7.4.sql" 2>/dev/null || true
        fi

        # Restart polaris-wvp so it reconnects cleanly with the guaranteed permissions
        $compose_cmd restart polaris-wvp 2>/dev/null || true

        echo ""
        log_info "Containers launched! Waiting for WVP Java Backend to finish booting (usually 20-30s)..."
        wvp_ready=false
        for i in {1..35}; do
            http_code=$(curl -s -o /dev/null -w "%{http_code}" -m 2 http://127.0.0.1:18978/api/user/login 2>/dev/null || true)
            if [ -n "$http_code" ] && [ "$http_code" != "000" ] && [ "$http_code" != "502" ]; then
                wvp_ready=true
                break
            fi
            sleep 2
        done

        echo ""
        if [ "$wvp_ready" = "true" ]; then
            log_success "WVP Master Control Plane is fully UP, READY, and LISTENING on port 18978!"
            log_info "• Web Dashboard (Nginx): http://localhost (hoặc http://localhost:80)"
            log_info "• WVP REST API Backend: http://localhost:18978"
            log_info "• SIP Signaling Port (GB28181): 8116 UDP/TCP"
            log_info "• Default login credentials: admin / admin"
            log_info "• Để thêm ZLMediaKit Worker: Dùng thư mục 'zlm-node' (nằm cạnh thư mục này) trên máy chủ Worker."
        else
            log_warning "WVP Backend is still booting or encountered a startup exception."
            log_info "Displaying recent backend logs for inspection:"
            if docker compose version &> /dev/null; then
                docker compose -f docker-compose-master.yml logs --tail=40 polaris-wvp
            else
                docker-compose -f docker-compose-master.yml logs --tail=40 polaris-wvp
            fi
        fi
    else
        log_error "Failed to start WVP Master services."
        echo ""
        log_warning "Fetching Redis & MySQL container logs for diagnosis:"
        if docker compose version &> /dev/null; then
            docker compose -f docker-compose-master.yml logs --tail=30 polaris-redis polaris-mysql polaris-wvp
        else
            docker-compose -f docker-compose-master.yml logs --tail=30 polaris-redis polaris-mysql polaris-wvp
        fi
        exit 1
    fi

elif [ "$mode_choice" = "2" ]; then
    if [ "$has_docker" = "false" ]; then
        log_error "Docker is required for this mode but it is not installed. Exiting."
        exit 1
    fi
    
    log_info "Preparing storage volumes and checking permissions..."
    mkdir -p "$SCRIPT_DIR/docker/volumes/redis/data" \
             "$SCRIPT_DIR/docker/volumes/mysql/data" \
             "$SCRIPT_DIR/docker/logs/wvp" \
             "$SCRIPT_DIR/docker/logs/mysql" \
             "$SCRIPT_DIR/docker/logs/nginx" \
             "$SCRIPT_DIR/docker/media/record" \
             "$SCRIPT_DIR/docker/logs/media" 2>/dev/null || true
    chmod -R 777 "$SCRIPT_DIR/docker/volumes" "$SCRIPT_DIR/docker/logs" "$SCRIPT_DIR/docker/media" 2>/dev/null || true

    # Clean stale/incompatible Redis AOF cache files (e.g. from previous redis:latest run)
    if [ -d "$SCRIPT_DIR/docker/volumes/redis/data/appendonlydir" ] || [ -f "$SCRIPT_DIR/docker/volumes/redis/data/dump.rdb" ]; then
        log_info "Cleaning stale Redis cache files to ensure clean database startup..."
        rm -rf "$SCRIPT_DIR/docker/volumes/redis/data" && mkdir -p "$SCRIPT_DIR/docker/volumes/redis/data" && chmod -R 777 "$SCRIPT_DIR/docker/volumes/redis/data" 2>/dev/null || true
    fi

    log_info "Starting All-in-One Full Stack with Docker Compose (kèm ZLMediaKit nội bộ)..."
    cd "$SCRIPT_DIR/docker"
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    
    # Clean up any previous stopped/unhealthy containers
    if docker compose version &> /dev/null; then
        docker compose down --remove-orphans 2>/dev/null || true
    else
        docker-compose down --remove-orphans 2>/dev/null || true
    fi

    # Check if MySQL data directory was corrupted by a previous interrupted initialization
    if [ -d "$SCRIPT_DIR/docker/volumes/mysql/data" ] && [ "$(ls -A "$SCRIPT_DIR/docker/volumes/mysql/data" 2>/dev/null)" ]; then
        if [ ! -d "$SCRIPT_DIR/docker/volumes/mysql/data/wvp" ] || [ -z "$(ls -A "$SCRIPT_DIR/docker/volumes/mysql/data/wvp" 2>/dev/null)" ]; then
            log_warning "Detected incomplete/corrupted MySQL initialization (database 'wvp' is empty or missing)."
            log_info "Auto-cleaning broken MySQL data files to allow a fresh, healthy initialization..."
            rm -rf "$SCRIPT_DIR/docker/volumes/mysql/data" && mkdir -p "$SCRIPT_DIR/docker/volumes/mysql/data" && chmod -R 777 "$SCRIPT_DIR/docker/volumes/mysql/data" 2>/dev/null || true
        fi
    fi

    log_info "Running: docker compose up -d --build --force-recreate"
    if docker compose version &> /dev/null; then
        docker compose up -d --build --force-recreate
    else
        docker-compose up -d --build --force-recreate
    fi
    
    if [ $? -eq 0 ]; then
        echo ""
        log_info "Synchronizing MySQL user permissions and database schema..."
        compose_cmd="docker compose -f docker-compose.yml"
        if ! docker compose version &>/dev/null; then
            compose_cmd="docker-compose -f docker-compose.yml"
        fi

        # Wait up to 30s for MySQL to be ready
        log_info "Waiting for MySQL service to accept connections..."
        for attempt in {1..15}; do
            if $compose_cmd exec -T polaris-mysql mysqladmin ping -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" --silent &>/dev/null; then
                break
            fi
            sleep 2
        done

        # Ensure wvp_user exists and has correct password in MySQL
        $compose_cmd exec -T polaris-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" -e "
            CREATE DATABASE IF NOT EXISTS wvp CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
            CREATE USER IF NOT EXISTS '${MYSQL_USER:-wvp_user}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD:-wvp_password}';
            ALTER USER '${MYSQL_USER:-wvp_user}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD:-wvp_password}';
            GRANT ALL PRIVILEGES ON wvp.* TO '${MYSQL_USER:-wvp_user}'@'%';
            FLUSH PRIVILEGES;
        " 2>/dev/null || true

        # Check if tables exist, if not import init-mysql.sql
        table_count=$($compose_cmd exec -T polaris-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'wvp';" 2>/dev/null || echo 0)
        if [ -z "$table_count" ] || [ "$table_count" -lt 5 ]; then
            log_info "Tables not found in 'wvp' database. Importing initial schema..."
            $compose_cmd exec -T polaris-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" wvp < "$SCRIPT_DIR/database/2.7.4/init-mysql-2.7.4.sql" 2>/dev/null || true
        fi

        # Restart polaris-wvp so it reconnects cleanly with the guaranteed permissions
        $compose_cmd restart polaris-wvp 2>/dev/null || true

        echo ""
        log_info "Containers launched! Waiting for WVP Java Backend to finish booting (usually 20-30s)..."
        wvp_ready=false
        for i in {1..35}; do
            http_code=$(curl -s -o /dev/null -w "%{http_code}" -m 2 http://127.0.0.1:18978/api/user/login 2>/dev/null || true)
            if [ -n "$http_code" ] && [ "$http_code" != "000" ] && [ "$http_code" != "502" ]; then
                wvp_ready=true
                break
            fi
            sleep 2
        done

        echo ""
        if [ "$wvp_ready" = "true" ]; then
            log_success "All-in-One services are fully UP, READY, and LISTENING on port 18978!"
            log_info "• WVP Web Dashboard (Nginx): http://localhost (hoặc http://localhost:80)"
            log_info "• WVP REST API Backend: http://localhost:18978"
            log_info "• Default login credentials: admin / admin"
        else
            log_warning "WVP Backend is still booting or encountered a startup exception."
            log_info "Displaying recent backend logs for inspection:"
            if docker compose version &> /dev/null; then
                docker compose logs --tail=40 polaris-wvp
            else
                docker-compose logs --tail=40 polaris-wvp
            fi
        fi
    else
        log_error "Failed to start Docker Compose services."
        echo ""
        log_warning "Fetching container logs for diagnosis:"
        if docker compose version &> /dev/null; then
            docker compose logs --tail=30 polaris-redis polaris-mysql polaris-wvp
        else
            docker-compose logs --tail=30 polaris-redis polaris-mysql polaris-wvp
        fi
        exit 1
    fi

elif [ "$mode_choice" = "3" ]; then
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

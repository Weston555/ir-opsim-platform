#!/bin/bash

# Industrial Robot Operation Simulation Platform - Deployment Script
# This script helps deploy the application using Docker Compose

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is installed and running
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker is not running. Please start Docker service."
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed."
        exit 1
    fi

    log_success "Docker and Docker Compose are available"
}

# Check if required ports are available
check_ports() {
    local ports=(80 8080 5432)

    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null; then
            log_warn "Port $port is already in use. Please stop the service using this port or change the port mapping."
        fi
    done
}

# Pull latest images
pull_images() {
    log_info "Pulling latest Docker images..."
    cd "$PROJECT_ROOT"
    docker compose pull
    log_success "Images pulled successfully"
}

# Build and start services
start_services() {
    log_info "Building and starting services..."
    cd "$PROJECT_ROOT"

    # Start services
    docker compose up -d --build

    log_success "Services started successfully"
}

# Wait for services to be healthy
wait_for_services() {
    log_info "Waiting for services to be healthy..."

    local max_attempts=60
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        echo -n "."

        # Check if all services are healthy
        if docker compose ps | grep -q "healthy"; then
            echo ""
            log_success "All services are healthy!"
            return 0
        fi

        sleep 5
        ((attempt++))
    done

    echo ""
    log_warn "Services are still starting. This may take a few more minutes."
    log_info "You can check the status with: docker compose ps"
    log_info "View logs with: docker compose logs -f"
}

# Show service status
show_status() {
    log_info "Service Status:"
    echo ""
    docker compose ps

    echo ""
    log_info "Access URLs:"
    echo "  Frontend:        http://localhost"
    echo "  Backend API:     http://localhost:8080/swagger-ui"
    echo "  Health Check:    http://localhost:8080/actuator/health"
    echo "  Database Admin:  http://localhost:5050 (optional)"
}

# Stop services
stop_services() {
    log_info "Stopping services..."
    cd "$PROJECT_ROOT"
    docker compose down
    log_success "Services stopped"
}

# Clean up
cleanup() {
    log_warn "Cleaning up containers, networks, and volumes..."
    cd "$PROJECT_ROOT"
    docker compose down -v --remove-orphans
    docker system prune -f
    log_success "Cleanup completed"
}

# Show usage
show_usage() {
    echo "Industrial Robot Operation Simulation Platform - Deployment Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start     Start all services (default)"
    echo "  stop      Stop all services"
    echo "  restart   Restart all services"
    echo "  status    Show service status"
    echo "  logs      Show service logs"
    echo "  cleanup   Clean up containers and volumes"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start    # Start the application"
    echo "  $0 logs     # View logs"
    echo "  $0 stop     # Stop the application"
}

# Main function
main() {
    local command="${1:-start}"

    case "$command" in
        start)
            log_info "Starting Industrial Robot Operation Simulation Platform..."
            check_docker
            check_ports
            pull_images
            start_services
            wait_for_services
            show_status
            ;;
        stop)
            stop_services
            ;;
        restart)
            log_info "Restarting services..."
            stop_services
            sleep 2
            start_services
            wait_for_services
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            log_info "Showing service logs (press Ctrl+C to exit)..."
            cd "$PROJECT_ROOT"
            docker compose logs -f
            ;;
        cleanup)
            cleanup
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            log_error "Unknown command: $command"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"

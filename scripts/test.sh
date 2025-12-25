#!/bin/bash

# Industrial Robot Operation Simulation Platform - Test Script
# This script runs various tests for the application

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

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

# Backend unit tests
test_backend() {
    log_info "Running backend unit tests..."
    cd "$PROJECT_ROOT/backend"

    if command -v mvn &> /dev/null; then
        mvn test
        log_success "Backend tests passed"
    else
        log_warn "Maven not found, skipping backend tests"
    fi
}

# Frontend unit tests
test_frontend() {
    log_info "Running frontend unit tests..."
    cd "$PROJECT_ROOT/frontend"

    if command -v npm &> /dev/null; then
        npm run test:unit
        log_success "Frontend tests passed"
    else
        log_warn "npm not found, skipping frontend tests"
    fi
}

# Integration tests using Docker Compose
test_integration() {
    log_info "Running integration tests..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is required for integration tests"
        return 1
    fi

    cd "$PROJECT_ROOT"

    # Start services for testing
    log_info "Starting services for integration testing..."
    docker compose up -d

    # Wait for services to be healthy
    log_info "Waiting for services to be ready..."
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_success "Services are ready for testing"
            break
        fi

        if [ $attempt -eq $max_attempts ]; then
            log_error "Services failed to start within timeout"
            docker compose logs backend
            docker compose down
            return 1
        fi

        sleep 10
        ((attempt++))
    done

    # Run API tests
    run_api_tests

    # Stop services
    log_info "Stopping test services..."
    docker compose down
}

# Run API tests
run_api_tests() {
    log_info "Running API integration tests..."

    # Test health endpoint
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        log_success "Health check passed"
    else
        log_error "Health check failed"
        return 1
    fi

    # Test authentication
    local token=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token' 2>/dev/null)

    if [ -n "$token" ] && [ "$token" != "null" ]; then
        log_success "Authentication test passed"
    else
        log_error "Authentication test failed"
        return 1
    fi

    # Test robots endpoint
    if curl -f -H "Authorization: Bearer $token" http://localhost:8080/api/v1/robots &> /dev/null; then
        log_success "Robots API test passed"
    else
        log_error "Robots API test failed"
        return 1
    fi

    log_success "All integration tests passed"
}

# Performance tests
test_performance() {
    log_info "Running performance tests..."

    if ! command -v k6 &> /dev/null; then
        log_warn "k6 not found, skipping performance tests"
        log_info "Install k6 from: https://k6.io/docs/get-started/installation/"
        return 0
    fi

    if [ ! -f "$PROJECT_ROOT/scripts/k6/performance-test.js" ]; then
        log_warn "Performance test script not found, creating basic test..."

        mkdir -p "$PROJECT_ROOT/scripts/k6"
        cat > "$PROJECT_ROOT/scripts/k6/performance-test.js" << 'EOF'
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up to 100 users over 2 minutes
    { duration: '5m', target: 100 }, // Stay at 100 users for 5 minutes
    { duration: '2m', target: 200 }, // Ramp up to 200 users over 2 minutes
    { duration: '5m', target: 200 }, // Stay at 200 users for 5 minutes
    { duration: '2m', target: 0 },   // Ramp down to 0 users over 2 minutes
  ],
  thresholds: {
    http_req_duration: ['p(99)<1500'], // 99% of requests must complete below 1.5s
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // Health check
  let response = http.get(`${BASE_URL}/actuator/health`);
  check(response, { 'status is 200': (r) => r.status === 200 });

  sleep(1);
}
EOF
    fi

    cd "$PROJECT_ROOT/scripts/k6"
    k6 run performance-test.js
}

# Run all tests
test_all() {
    log_info "Running all tests..."

    test_backend
    test_frontend
    test_integration
    test_performance

    log_success "All tests completed successfully!"
}

# Show usage
show_usage() {
    echo "Industrial Robot Operation Simulation Platform - Test Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  all        Run all tests (default)"
    echo "  backend    Run backend unit tests"
    echo "  frontend   Run frontend unit tests"
    echo "  integration Run integration tests"
    echo "  performance Run performance tests"
    echo "  help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 all         # Run all tests"
    echo "  $0 backend     # Run only backend tests"
    echo "  $0 integration # Run integration tests"
}

# Main function
main() {
    local command="${1:-all}"

    case "$command" in
        all)
            test_all
            ;;
        backend)
            test_backend
            ;;
        frontend)
            test_frontend
            ;;
        integration)
            test_integration
            ;;
        performance)
            test_performance
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

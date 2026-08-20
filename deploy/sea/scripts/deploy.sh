#!/bin/bash
# =============================================================================
# Apache Fineract - Southeast Asia Deployment Script
# Usage: ./deploy.sh [ph|id|kh] [docker|k8s]
# =============================================================================
set -euo pipefail

REGION="${1:-ph}"
DEPLOYMENT_MODE="${2:-docker}"
PROJECT_NAME="fineract-sea"
IMAGE_TAG="${IMAGE_TAG:-latest}"
REGISTRY_URL="${REGISTRY_URL:-registry.your-domain.com}"

echo "=============================================="
echo " Apache Fineract SEA Deployment"
echo " Region: $REGION"
echo " Mode: $DEPLOYMENT_MODE"
echo "=============================================="

# ---- Validate region ----
case $REGION in
    ph) COUNTRY_NAME="Philippines" COUNTRY_CODE="ph" ;;
    id) COUNTRY_NAME="Indonesia" COUNTRY_CODE="id" ;;
    kh) COUNTRY_NAME="Cambodia" COUNTRY_CODE="kh" ;;
    *) echo "Error: Unknown region '$REGION'. Use: ph, id, or kh"; exit 1 ;;
esac

ENV_FILE="env/fineract-${COUNTRY_CODE}-prod.env"

# ---- Load environment ----
echo "Loading environment from: $ENV_FILE"
if [ ! -f "$ENV_FILE" ]; then
    echo "Error: Environment file not found: $ENV_FILE"
    exit 1
fi

# ---- Build Docker Image ----
build_image() {
    echo "Building Docker image..."
    cd ../..
    ./gradlew :fineract-provider:build -x test -x checkstyleMain

    docker build \
        -t "${REGISTRY_URL}/${PROJECT_NAME}:${IMAGE_TAG}" \
        -f Dockerfile .

    echo "Docker image built: ${REGISTRY_URL}/${PROJECT_NAME}:${IMAGE_TAG}"
}

# ---- Docker Compose Deployment ----
deploy_docker() {
    echo "Deploying via Docker Compose for $COUNTRY_NAME..."

    # Create secrets directory if not exists
    mkdir -p env/nginx-certs

    # Check required secrets
    if [ ! -f "env/nginx-certs/server.crt" ]; then
        echo "WARNING: SSL certificate not found at env/nginx-certs/server.crt"
        echo "Generate certificates:"
        echo "  openssl req -x509 -newkey rsa:4096 -keyout server.key -out server.crt -days 365"
    fi

    # Create Docker secrets
    echo "Setting up Docker secrets..."
    echo "your_db_password" | docker secret create fineract_db_password - || true

    # Deploy
    export COUNTRY=$COUNTRY_CODE
    export REDIS_PASSWORD=$(openssl rand -base64 16)
    export KAFKA_CLUSTER_ID=$(openssl rand -hex 16)

    docker compose \
        -f docker/docker-compose-sea-production.yml \
        --env-file "$ENV_FILE" \
        up -d --build

    echo "Deployment complete!"
    echo "Services:"
    docker compose -f docker/docker-compose-sea-production.yml ps
}

# ---- Kubernetes Deployment ----
deploy_k8s() {
    echo "Deploying to Kubernetes for $COUNTRY_NAME..."

    # Check kubectl context
    echo "Current kubectl context: $(kubectl config current-context)"
    echo "Continuing in 5 seconds... (Ctrl+C to cancel)"
    sleep 5

    # Create namespace
    kubectl create namespace fineract-prod --dry-run=client -o yaml | kubectl apply -f -

    # Create secrets (requires manual input for production)
    echo "Creating secrets..."
    kubectl create secret generic fineract-db-secret \
        --from-literal=username=fineract \
        --from-literal=password="$(openssl rand -base64 32)" \
        -n fineract-prod --dry-run=client -o yaml | kubectl apply -f -

    # Apply ConfigMap
    kubectl apply -f k8s/fineract-sea-production.yml

    # Build and push image
    build_image
    docker push "${REGISTRY_URL}/${PROJECT_NAME}:${IMAGE_TAG}"

    # Apply deployment
    kubectl apply -f k8s/fineract-sea-production.yml

    # Wait for rollout
    echo "Waiting for rollout to complete..."
    kubectl rollout status deployment/fineract-server -n fineract-prod --timeout=300s

    # Check health
    echo "Checking health..."
    kubectl get pods -n fineract-prod -l app=fineract-server

    echo ""
    echo "=============================================="
    echo " Deployment Complete!"
    echo "=============================================="
    echo "kubectl port-forward svc/fineract-server 8443:8443 -n fineract-prod"
    echo ""
}

# ---- Stop Deployment ----
stop_deployment() {
    echo "Stopping deployment..."
    if [ "$DEPLOYMENT_MODE" = "docker" ]; then
        docker compose \
            -f docker/docker-compose-sea-production.yml \
            --env-file "$ENV_FILE" \
            down
    else
        kubectl delete namespace fineract-prod
    fi
    echo "Deployment stopped."
}

# ---- Main ----
case "${3:-}" in
    stop)
        stop_deployment
        ;;
    *)
        case "$DEPLOYMENT_MODE" in
            docker)
                deploy_docker
                ;;
            k8s)
                deploy_k8s
                ;;
            *)
                echo "Error: Unknown deployment mode '$DEPLOYMENT_MODE'. Use: docker or k8s"
                exit 1
                ;;
        esac
        ;;
esac

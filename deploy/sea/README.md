# Apache Fineract - Southeast Asia Digital Banking Deployment

Production-ready deployment configurations and payment gateway implementations for deploying Apache Fineract as a digital banking platform in Southeast Asia (Philippines, Indonesia, Cambodia).

## Directory Structure

```
deploy/sea/
├── docker/
│   └── docker-compose-sea-production.yml   # Production Docker Compose
├── k8s/
│   └── fineract-sea-production.yml          # Kubernetes manifests (EKS/GKE)
├── env/
│   ├── fineract-sea-common.env             # Shared SEA configuration
│   ├── fineract-ph-prod.env                # Philippines settings
│   ├── fineract-id-prod.env                # Indonesia settings
│   ├── fineract-kh-prod.env                # Cambodia settings
│   └── nginx-sea.conf                       # Nginx reverse proxy config
├── payments/
│   └── gcash/                              # GCash (Philippines) payment gateway
│       ├── GCashGateway.java
│       ├── GCashProperties.java
│       ├── GCashPaymentRequest.java
│       └── GCashPaymentResponse.java
└── scripts/
    ├── deploy.sh                           # Deployment automation
    └── OPERATIONS_RUNBOOK.md               # Operational procedures
```

## Quick Start

### 1. Philippines Deployment (Docker)

```bash
cd deploy/sea
export COUNTRY=ph
docker compose -f docker/docker-compose-sea-production.yml --env-file env/fineract-ph-prod.env up -d
```

### 2. Philippines Deployment (Kubernetes)

```bash
cd deploy/sea
kubectl apply -f k8s/fineract-sea-production.yml
kubectl rollout status deployment/fineract-server -n fineract-prod
```

### 3. Switch to Indonesia

```bash
export COUNTRY=id
# Update env/fineract-id-prod.env with production values
docker compose -f docker/docker-compose-sea-production.yml --env-file env/fineract-id-prod.env up -d
```

## Supported Payment Gateways

| Country | Payment Method | Status |
|---------|---------------|--------|
| Philippines | GCash | Implemented (template) |
| Philippines | Maya (PayMaya) | Configurable |
| Philippines | PESONet | Configurable |
| Philippines | UnionPay | Built-in |
| Indonesia | QRIS | Configurable |
| Indonesia | Gojek Pay | Configurable |
| Indonesia | OVO | Configurable |
| Indonesia | ShopeePay | Configurable |
| Cambodia | Wing Money | Configurable |
| Cambodia | PiPay | Configurable |
| Cambodia | CIPS | Configurable |

## Prerequisites

- Java 21+ (Zulu)
- Docker Engine 24+ / Kubernetes 1.28+
- PostgreSQL 16+
- SSL certificates (Let's Encrypt or commercial)
- Domain name and DNS configuration
- Cloud provider account (AWS/GCP)
- Payment gateway merchant accounts

## Security Notes

- **Never** commit actual passwords or secrets to version control
- Use Docker secrets or Kubernetes Secrets for sensitive data
- Enable TLS for all external-facing endpoints
- Regularly rotate database and API credentials
- Follow regulatory data retention requirements

## Documentation

- See [OPERATIONS_RUNBOOK.md](scripts/OPERATIONS_RUNBOOK.md) for daily operational procedures
- See parent project documentation for Fineract API reference
- Apache Fineract: https://fineract.apache.org/

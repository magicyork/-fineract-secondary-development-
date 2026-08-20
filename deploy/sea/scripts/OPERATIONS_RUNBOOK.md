# =============================================================================
# Apache Fineract SEA - Production Operations Runbook
# =============================================================================
#
# This runbook covers daily operational procedures for maintaining
# the Fineract digital banking platform in Southeast Asian production
# environments (Philippines, Indonesia, Cambodia).
#

# =============================================================================
# 1. DAILY HEALTH CHECKLIST
# =============================================================================

## 1.1 Service Health (Every Hour)

```bash
# Docker Compose
docker compose -f docker/docker-compose-sea-production.yml ps
docker compose -f docker/docker-compose-sea-production.yml logs --tail=50 fineract-manager

# Kubernetes
kubectl get pods -n fineract-prod
kubectl get pods -n fineract-prod -l app=fineract-server
kubectl logs -n fineract-prod deployment/fineract-server --tail=100

# Health Endpoints
curl -k https://localhost:8443/fineract-provider/actuator/health
curl -k https://localhost:8443/fineract-provider/actuator/health/liveness
curl -k https://localhost:8443/fineract-provider/actuator/health/readiness
```

## 1.2 Database Health

```bash
# PostgreSQL Primary
psql -h localhost -p 5432 -U fineract -d fineract_tenants -c "
SELECT
    count(*) as total_connections,
    state,
    query_start,
    query
FROM pg_stat_activity
WHERE datname = 'fineract_tenants'
GROUP BY state
ORDER BY state;
"

# Check for long-running queries
psql -h localhost -p 5432 -U fineract -d fineract_tenants -c "
SELECT pid, now() - query_start as duration, query
FROM pg_stat_activity
WHERE state = 'active'
AND now() - query_start > interval '5 minutes'
ORDER BY duration DESC;
"

# Database size
psql -h localhost -p 5432 -U fineract -d fineract_tenants -c "
SELECT
    pg_size_pretty(pg_database_size('fineract_tenants')) as db_size;
"

# Connection pool status (via API)
curl -k https://localhost:8443/fineract-provider/actuator/metrics/hikaricp.connections
```

## 1.3 Kafka Health

```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Lag check (for event processors)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
    --describe --group fineract-consumer-group

# Topic sizes
kafka-log-dirs.sh --bootstrap-server localhost:9092 \
    --describe --topic-list "external-events,job-topic"
```

## 1.4 Redis Health

```bash
redis-cli -a $REDIS_PASSWORD ping
redis-cli -a $REDIS_PASSWORD info memory
redis-cli -a $REDIS_PASSWORD info stats
redis-cli -a $REDIS_PASSWORD dbsize
```

# =============================================================================
# 2. BATCH JOB MONITORING (Daily)
# =============================================================================

## 2.1 Check Cob (Cost-of-Business) Processing

```bash
# Trigger interest calculation (via API)
curl -X POST -k https://localhost:8443/fineract-provider/api/v1/loans/commands/advanceLoanDate/{loanId}

# Or via batch job
curl -X POST -k https://localhost:8443/fineract-provider/api/v1/jobs/loan-cob

# Check batch job status
curl -k https://localhost:8443/fineract-provider/api/v1/jobs/{jobId}

# Failed job list
curl -k https://localhost:8443/fineract-provider/api/v1/jobs?status=FAILED
```

## 2.2 Interest Posting

```bash
# Trigger savings interest posting
curl -X POST -k https://localhost:8443/fineract-provider/api/v1/savingsaccounts/{id}?command=postInterest

# Batch interest posting
curl -X POST -k https://localhost:8443/fineract-provider/api/v1/jobs/savings-interest-posting
```

## 2.3 Payment Processing Reconciliation

```bash
# Query pending payments
curl -k https://localhost:8443/fineract-provider/api/v1/payments?status=PENDING

# Retry failed payments
curl -X POST -k https://localhost:8443/fineract-provider/api/v1/payments/{id}?command=retry

# Payment success rate (via Prometheus)
# Check: fineract_payment_success_total / fineract_payment_total
```

# =============================================================================
# 3. BACKUP PROCEDURES
# =============================================================================

## 3.1 PostgreSQL Backup (Daily, 2 AM Local Time)

```bash
# Manual backup
pg_dump -h localhost -U fineract -d fineract_tenants \
    | gzip > /backup/fineract_tenants_$(date +%Y%m%d_%H%M%S).sql.gz

# Restore test (validate backup integrity)
gunzip -c /backup/fineract_tenants_*.sql.gz \
    | psql -h localhost -U fineract -d fineract_tenants_restore

# Automated backup cron (add to crontab)
# 0 2 * * * pg_dump -h localhost -U fineract -d fineract_tenants | gzip > /backup/fineract_$(date +\%Y\%m\%d).sql.gz
```

## 3.2 Log Backup

```bash
# Archive application logs
tar -czf logs_$(date +%Y%m%d).tar.gz /var/logs/fineract/

# Upload to S3
aws s3 cp logs_*.tar.gz s3://fineract-sea-logs/backup/$(date +%Y/%m/%d)/
```

# =============================================================================
# 4. INCIDENT RESPONSE
# =============================================================================

## 4.1 High API Latency

```bash
# Check API latency via Prometheus
# Query: rate(http_server_requests_seconds_sum[5m])/rate(http_server_requests_seconds_count[5m])

# Database slow queries
psql -c "
SELECT pid, state, query_start, query,
       now() - query_start as duration
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY duration DESC
LIMIT 10;
"

# Connection pool exhaustion check
# Look for: HikariPool - Connection is not available
# Check: fineract_hikaricp_connections_idle
```

## 4.2 Service Downtime

```bash
# 1. Check container status
docker compose ps
kubectl get pods -n fineract-prod

# 2. Check recent logs
docker compose logs --tail=200 fineract-manager
kubectl logs -n fineract-prod deployment/fineract-server --tail=200 --previous

# 3. Check database connectivity
psql -h localhost -U fineract -d fineract_tenants -c "SELECT 1"

# 4. Check Kafka connectivity
kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# 5. Restart services (if needed)
docker compose restart fineract-manager
kubectl rollout restart deployment/fineract-server -n fineract-prod
```

## 4.3 Payment Gateway Failures

```bash
# Check payment gateway status
curl -k https://localhost:8443/fineract-provider/actuator/info

# Query recent payment failures
curl -k https://localhost:8443/fineract-provider/api/v1/payments?status=FAILED&dateFrom=$(date -d '1 hour ago' +%Y-%m-%d)

# GCash API status
curl -X GET "https://api.gcash.com/merchant/v1/status" \
    -H "X-GCASH-PARTNER-ID: $GCASH_PARTNER_ID"

# Maya API status
curl -X GET "https://api.paymaya.com/paymayav2/status" \
    -H "Authorization: Bearer $MAYA_TOKEN"
```

# =============================================================================
# 5. SCALING PROCEDURES
# =============================================================================

## 5.1 Horizontal Scaling (API Servers)

```bash
# Docker Compose
docker compose -f docker/docker-compose-sea-production.yml up -d --scale fineract-manager=5

# Kubernetes
kubectl scale deployment fineract-server --replicas=5 -n fineract-prod
kubectl autoscale deployment fineract-server \
    --min=3 --max=10 \
    --cpu-percent=70 \
    -n fineract-prod
```

## 5.2 Database Scaling

```bash
# Add read replica (for read-heavy workloads)
docker compose -f docker/docker-compose-sea-production.yml up -d db-replica

# Or promote replica to primary (during failure)
# pg_ctlcluster promote
```

## 5.3 Connection Pool Tuning

```bash
# Adjust HikariCP settings via environment variables
FINERACT_HIKARI_MAXIMUM_POOL_SIZE=100
FINERACT_HIKARI_MINIMUM_IDLE=20

# Apply changes
docker compose up -d fineract-manager
```

# =============================================================================
# 6. SECURITY PROCEDURES
# =============================================================================

## 6.1 Daily Security Checks

```bash
# Check for failed login attempts
grep -i "failed" /var/logs/fineract/security.log | tail -100

# Check for unauthorized access
grep -i "unauthorized\|forbidden" /var/log/nginx/access.log | tail -50

# Scan for vulnerabilities (weekly)
trivy image registry.your-domain.com/fineract-sea:latest
grype registry.your-domain.com/fineract-sea:latest
```

## 6.2 Certificate Rotation

```bash
# Let's Encrypt (if using)
certbot renew --dry-run

# Manual rotation
# 1. Generate new certs
openssl req -x509 -newkey rsa:4096 \
    -keyout new-key.pem \
    -out new-cert.pem \
    -days 365

# 2. Deploy
kubectl create secret tls fineract-tls \
    --cert=new-cert.pem --key=new-key.pem \
    -n fineract-prod --dry-run=client -o yaml | kubectl apply -f -

# 3. Restart
kubectl rollout restart deployment fineract-server -n fineract-prod
```

## 6.3 Database Password Rotation

```bash
# 1. Set new password
export NEW_DB_PASSWORD=$(openssl rand -base64 32)

# 2. Update PostgreSQL
psql -c "ALTER USER fineract PASSWORD '$NEW_DB_PASSWORD';"

# 3. Update application secrets
kubectl create secret generic fineract-db-secret \
    --from-literal=username=fineract \
    --from-literal=password=$NEW_DB_PASSWORD \
    -n fineract-prod --dry-run=client -o yaml | kubectl apply -f -

# 4. Rolling restart
kubectl rollout restart deployment fineract-server -n fineract-prod
```

# =============================================================================
# 7. REGULATORY COMPLIANCE CHECKS
# =============================================================================

## 7.1 Philippines (BSP)

- **AML Report**: Monthly SAR (Suspicious Activity Report) submission
- **Transaction Report**: Daily large transaction report (> PHP 500,000)
- **Data Retention**: 5 years for transaction records
- **Audit Trail**: Complete audit log for all customer actions

```bash
# Generate BSP compliance report
curl -X POST -k https://localhost:8443/fineract-provider/api/v1/reports/generate \
    -H "Content-Type: application/json" \
    -d '{"reportType":"BSP_MONTHLY","period":"2026-08"}'
```

## 7.2 Indonesia (OJK)

- **KYC Verification**: National ID (KTP) verification required
- **Transaction Limits**: Monthly transaction limits per customer tier
- **SLA**: Max 24 hours for credit decision, 48 hours for disbursement
- **Reporting**: Monthly reports to OJK

## 7.3 Cambodia (NBC)

- **Foreign Exchange**: Strict FX controls, NBC approval for cross-border
- **Credit Limits**: Interest rate caps and fee limitations
- **Reporting**: Quarterly financial reports to NBC

# =============================================================================
# 8. CONTACT & ESCALATION
# =============================================================================

| Level | Response Time | Contact |
|-------|-------------|---------|
| **P1 - Critical** (System Down) | < 15 min | DevOps on-call + Team Lead |
| **P2 - High** (Payment Failures) | < 1 hour | DevOps + Backend Team |
| **P3 - Medium** (Performance Degradation) | < 4 hours | DevOps Team |
| **P4 - Low** (Minor Issues) | < 24 hours | Next business day |

---
*This runbook should be reviewed and updated quarterly.*

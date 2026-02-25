---
name: deployment
description: Deployment plans with strategy implementation, zero-downtime migration ordering, smoke tests, and rollback procedures
activation:
  keywords: ["deploy", "deployment", "release", "rollback", "ship", "go live", "production", "staging"]
  file_patterns: ["**/deploy/**", "**/deployment/**", "**/k8s/**", "**/terraform/**"]
---

# Deployment Guide

## Purpose
Create deployment plans with checklists, environment configuration, strategy-specific implementation, and tested rollback procedures.

## Pre-Deployment Checklist

```
- [ ] All tests passing on the target branch (link to CI run)
- [ ] Database migrations tested against a copy of production data
- [ ] Environment variables configured and verified for target environment
- [ ] Secrets rotated if any were potentially exposed
- [ ] Dependencies audited: npm audit / pip audit / cargo audit
- [ ] Performance tested under expected load (if traffic-sensitive)
- [ ] Rollback procedure documented and tested in staging
- [ ] Team notified of deployment window
- [ ] Monitoring dashboards open and baseline noted
- [ ] On-call engineer aware of deployment
```

---

## Zero-Downtime: Migration + Code Ordering

The hardest part of zero-downtime deployment is coordinating schema changes with code. The rule: **the database must be compatible with both the old and new code simultaneously**.

### Safe ordering for adding a column

```
1. Deploy migration: ADD COLUMN new_col NULL  ← no NOT NULL yet; old code ignores it
2. Deploy new code that writes to new_col
3. Backfill: UPDATE table SET new_col = ... WHERE new_col IS NULL
4. Deploy migration: ALTER COLUMN new_col SET NOT NULL
```

### Safe ordering for renaming a column

```
1. Deploy migration: ADD COLUMN new_name (copy of old_name)
2. Deploy new code that writes to BOTH old_name and new_name
3. Backfill: UPDATE SET new_name = old_name WHERE new_name IS NULL
4. Deploy new code that reads new_name, writes only new_name
5. Deploy migration: DROP COLUMN old_name
```

### Safe ordering for dropping a column
```
1. Deploy new code that no longer reads or writes the column
2. Deploy migration: DROP COLUMN  ← safe now, no code references it
```

---

## Deployment Strategies

### Rolling (default)

Replace instances one at a time. Traffic shifts gradually as new instances come up.

**Kubernetes:**
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0    # never reduce capacity
    maxSurge: 1          # one extra instance during rollout
```

```bash
kubectl set image deployment/api api=myapp:v1.2.3
kubectl rollout status deployment/api          # watch progress
kubectl rollout undo deployment/api            # rollback
```

**Fly.io:**
```bash
fly deploy --strategy rolling
```

### Blue/Green

Run two identical environments. Switch all traffic at once (instant, zero-downtime, easy rollback).

```bash
# AWS with target groups
# Blue = current production TG, Green = new version TG

# 1. Deploy new version to green
aws ecs update-service --cluster prod --service api-green --task-definition api:42

# 2. Wait for green to be healthy
aws ecs wait services-stable --cluster prod --services api-green

# 3. Switch load balancer listener to green
aws elbv2 modify-rule \
  --rule-arn $LISTENER_RULE_ARN \
  --actions Type=forward,TargetGroupArn=$GREEN_TG_ARN

# 4. Rollback: switch back to blue (instant)
aws elbv2 modify-rule \
  --rule-arn $LISTENER_RULE_ARN \
  --actions Type=forward,TargetGroupArn=$BLUE_TG_ARN
```

### Canary

Route a small percentage of traffic to the new version. Increase gradually as confidence grows.

```yaml
# Kubernetes with nginx-ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    nginx.ingress.kubernetes.io/canary: "true"
    nginx.ingress.kubernetes.io/canary-weight: "10"   # 10% to canary
```

**Rollout ladder**: 1% → 5% → 25% → 50% → 100%
Wait at each step: watch error rate and p99 latency. Abort if either degrades.

```bash
# Update canary weight
kubectl annotate ingress api-canary \
  nginx.ingress.kubernetes.io/canary-weight="25" --overwrite

# Promote: remove canary annotation, update main deployment
kubectl delete ingress api-canary
kubectl set image deployment/api api=myapp:v1.2.3
```

---

## Smoke Test Script

Run immediately after deployment. Tests the critical path, not the full suite.

```bash
#!/usr/bin/env bash
# smoke-test.sh — run after every deployment
set -euo pipefail

BASE_URL="${BASE_URL:-https://api.example.com}"
FAIL=0

check() {
  local name="$1" url="$2" expected_status="$3"
  actual=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  if [[ "$actual" == "$expected_status" ]]; then
    echo "  PASS  $name ($actual)"
  else
    echo "  FAIL  $name — expected $expected_status got $actual"
    FAIL=1
  fi
}

echo "Smoke tests: $BASE_URL"
check "health endpoint"  "$BASE_URL/health"         200
check "API root"         "$BASE_URL/api/v1"          200
check "auth required"    "$BASE_URL/api/v1/me"       401
check "not found shape"  "$BASE_URL/api/nonexistent" 404

if [[ $FAIL -eq 1 ]]; then
  echo "SMOKE TESTS FAILED — trigger rollback"
  exit 1
fi
echo "All smoke tests passed."
```

---

## Rollback Triggers and Procedure

**Trigger rollback if within 15 minutes of deploy:**
- Error rate > 1% (was < 0.1% baseline)
- p99 latency > 2× baseline
- Any smoke test failure
- On-call gets paged

**Rollback commands:**

```bash
# Kubernetes
kubectl rollout undo deployment/api
kubectl rollout status deployment/api     # confirm rollback completed

# Docker Compose / VPS — keep previous image tagged
docker tag myapp:v1.2.3 myapp:rollback-v1.2.3
docker pull myapp:v1.2.2                  # previous version
docker-compose up -d --no-deps api

# Fly.io
fly releases list                         # find previous version
fly deploy --image registry.fly.io/myapp:v1.2.2

# Database rollback (if migration is reversible)
npm run migrate:rollback
# or
alembic downgrade -1
```

---

## Platform Quick Reference

| Platform | Deploy command | Rollback |
|----------|---------------|----------|
| Kubernetes | `kubectl set image` / `helm upgrade` | `kubectl rollout undo` |
| Fly.io | `fly deploy` | `fly deploy --image <prev>` |
| Railway | `railway up` | Redeploy previous in dashboard |
| Render | Push to branch | Rollback in dashboard |
| AWS ECS | `ecs update-service` | `ecs update-service --task-definition <prev>` |
| Vercel | `vercel --prod` | `vercel rollback` |
| Netlify | `netlify deploy --prod` | Redeploy previous in dashboard |

---
name: cost-analysis
description: Analyze and optimize infrastructure costs with discovery steps, right-sizing thresholds, and break-even calculations
activation:
  keywords: ["cost analysis", "cost optimization", "cloud costs", "infrastructure cost", "billing", "budget", "cost estimate", "aws cost", "gcp cost", "azure cost"]
  file_patterns: ["**/terraform/**", "**/infrastructure/**", "**/cloudformation/**"]
---

# Infrastructure Cost Analysis

## Purpose
Inventory infrastructure resources, estimate costs, identify concrete optimization opportunities, and project costs at different scales.

## Step 1: Generate the Resource Inventory

Don't start from memory — pull the actual inventory from the provider.

**AWS:**
```bash
# All EC2 instances with type and state
aws ec2 describe-instances \
  --query 'Reservations[].Instances[].[InstanceId,InstanceType,State.Name,Tags[?Key==`Name`].Value|[0]]' \
  --output table

# RDS instances
aws rds describe-db-instances \
  --query 'DBInstances[].[DBInstanceIdentifier,DBInstanceClass,Engine,DBInstanceStatus]' \
  --output table

# Unattached EBS volumes (likely wasted money)
aws ec2 describe-volumes \
  --filters Name=status,Values=available \
  --query 'Volumes[].[VolumeId,Size,VolumeType]' \
  --output table

# S3 bucket sizes (use Cost Explorer for accurate numbers)
aws s3 ls --recursive s3://bucket-name --summarize 2>/dev/null | tail -2

# Current month spend by service
aws ce get-cost-and-usage \
  --time-period Start=$(date -d "$(date +%Y-%m-01)" +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --group-by Type=DIMENSION,Key=SERVICE
```

**GCP:**
```bash
# All Compute instances
gcloud compute instances list --format="table(name,machineType,status,zone)"

# BigQuery: estimated costs
bq ls --format=prettyjson | jq '.[].tableReference.tableId'
```

**From Terraform state:**
```bash
terraform show -json | jq '.values.root_module.resources[] | {type: .type, name: .name}'
```

---

## Step 2: Right-Sizing — Thresholds

Pull 2-week CPU and memory averages before making decisions.

```bash
# AWS CloudWatch: average CPU for an EC2 instance (last 14 days)
aws cloudwatch get-metric-statistics \
  --namespace AWS/EC2 \
  --metric-name CPUUtilization \
  --dimensions Name=InstanceId,Value=i-1234567890abcdef0 \
  --start-time $(date -u -d '14 days ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 1209600 \
  --statistics Average \
  --query 'Datapoints[0].Average'
```

**Decision thresholds:**

| Avg CPU | Avg Memory | Action |
|---------|-----------|--------|
| < 10% | < 20% | Downsize 2 instance sizes (e.g., m5.xlarge → m5.small) |
| 10–20% | 20–40% | Downsize 1 size |
| 20–60% | 40–70% | Correctly sized — no change |
| > 70% | > 80% | Upsize or scale horizontally |
| Spiky (< 5% avg, occasional 80%+ peaks) | — | Consider burstable (t3/t4g) or autoscaling |

**Savings estimate:** Each AWS instance size step down saves ~50% of that resource's cost.

---

## Step 3: Reserved Instance Break-Even

Reserved Instances (RIs) or Savings Plans make sense for predictable workloads.

**Break-even formula:**
```
Months to break even = Upfront cost / (On-demand monthly - RI monthly)

Example: m5.large in us-east-1
  On-demand:          $0.096/hr  = $70/month
  1-year RI (no upfront): $0.061/hr  = $45/month  → saves $25/mo → break-even: immediate
  1-year RI (all upfront): $530 upfront + $0/hr    → break-even: 530/25 = 21 months ← too long
  3-year RI (all upfront): $800 upfront + $0/hr    → saves $70/mo → break-even: 11 months ✓
```

**Rule of thumb:**
- No-upfront 1-year RI: almost always worth it for any instance running >50% of the time
- All-upfront: only worth it if you're confident the instance runs for the full term
- Savings Plans (AWS) are more flexible — apply to any instance family in a region

---

## Step 4: Identify Quick Wins

Work through these in order (easiest → highest impact):

1. **Unattached resources** — EBS volumes, Elastic IPs, load balancers with no targets
   - Cost: $0.10/GB/month for EBS, $0.005/hr for idle EIP
   - Fix: `aws ec2 release-address --allocation-id <id>`

2. **Idle instances** — < 5% CPU for 14+ days
   - Fix: stop or terminate if not needed; right-size if occasionally used

3. **Oversized databases** — same CPU/memory thresholds as compute
   - RDS: modify instance class during maintenance window

4. **Old snapshots** — automated snapshots older than your retention policy
   ```bash
   aws ec2 describe-snapshots --owner-ids self \
     --query 'Snapshots[?StartTime<=`2024-01-01`].[SnapshotId,StartTime,VolumeSize]' \
     --output table
   ```

5. **Spot/preemptible for batch workloads** — CI runners, ML training, data pipelines
   - Spot savings: 60–90% off on-demand
   - Requirement: workload tolerates interruption (can checkpoint/retry)

6. **Storage tiering** — S3 Intelligent-Tiering, Glacier for backups >90 days old
   - S3 Standard: $0.023/GB → Glacier: $0.004/GB

7. **Data transfer** — largest hidden cost; keep traffic in-region, use CDN for assets

---

## Output Format

```markdown
## Current Monthly Cost Estimate

| Resource | Type | Count | $/mo |
|----------|------|-------|------|
| Web servers | t3.medium | 3 | $90 |
| Database | RDS db.r5.large | 1 | $185 |
| Load balancer | ALB | 1 | $22 |
| S3 (static assets) | Standard 50GB | — | $1.15 |
| **Total** | | | **$298** |

## Right-Sizing Findings

| Instance | Avg CPU | Avg Mem | Current | Recommended | Monthly Savings |
|----------|---------|---------|---------|-------------|-----------------|
| web-01 | 8% | 15% | t3.medium | t3.small | $15 |
| web-02 | 7% | 18% | t3.medium | t3.small | $15 |

## Quick Wins (< 1 hour to implement)

| Action | Monthly Savings | Risk |
|--------|----------------|------|
| Delete 3 unattached EBS volumes (120 GB) | $12 | None |
| Release 2 unused Elastic IPs | $7 | None |
| Right-size web-01, web-02 | $30 | Low |

## Reserved Instance Recommendation

| Resource | Action | Upfront | Monthly Savings | Break-even |
|----------|--------|---------|-----------------|------------|
| RDS db.r5.large | 1-yr RI, no upfront | $0 | $55 | Immediate |

## Cost at Scale

| Traffic | Monthly Cost | Notes |
|---------|-------------|-------|
| Current (baseline) | $298 | |
| 2× traffic | $388 | +1 web server |
| 5× traffic | $598 | +3 web servers, larger RDS |
| 10× traffic | $1,200 | Consider read replica, CDN |
```

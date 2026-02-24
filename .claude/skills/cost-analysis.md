---
name: cost-analysis
description: Analyze and optimize infrastructure costs with comparison tables
activation:
  keywords: ["cost analysis", "cost optimization", "cloud costs", "infrastructure cost", "billing", "budget", "cost estimate"]
  file_patterns: ["**/terraform/**", "**/infrastructure/**", "**/cloudformation/**"]
---

# Infrastructure Cost Analysis

## Purpose
Inventory infrastructure resources, estimate costs, identify optimization opportunities, and project costs at different scales.

## Instructions

1. **Inventory current resources**:
   - Compute: instances, containers, serverless functions (type, count, region)
   - Storage: block storage, object storage, databases (size, IOPS, backups)
   - Networking: load balancers, CDN, data transfer, DNS, VPN
   - Managed services: search, cache, message queues, email, monitoring
   - Third-party SaaS: CI/CD, error tracking, APM, logging

2. **Estimate monthly costs for each resource**:
   - Use current pricing from the cloud provider
   - Account for: compute hours, storage GB, data transfer GB, API calls
   - Include reserved/committed pricing if applicable
   - Factor in free tier allowances
   - Note per-environment costs (dev/staging/prod)

3. **Identify optimization opportunities**:
   - **Right-sizing**: Are instances over-provisioned? Check CPU/memory utilization.
   - **Reserved instances**: Predictable workloads can save 30-60% with commitments
   - **Spot/preemptible**: Fault-tolerant workloads (batch, CI) can use spot instances
   - **Serverless migration**: Low-traffic services may be cheaper as Lambda/Cloud Functions
   - **Storage tiering**: Move infrequently accessed data to cheaper storage classes
   - **Data transfer**: Use CDN, compress responses, keep traffic in-region
   - **Unused resources**: Identify idle instances, unattached volumes, old snapshots

4. **Compare alternatives**:
   - Compare cloud providers for the specific workload
   - Compare managed vs self-hosted for key services
   - Compare instance families and generations
   - Calculate break-even points for reserved vs on-demand

5. **Project costs at scale**:
   - Estimate costs at 2x, 5x, 10x current traffic
   - Identify which costs scale linearly vs step-function
   - Note where architectural changes would reduce cost at scale

## Output Format

```markdown
## Current Monthly Cost Estimate

| Resource | Type | Count | Monthly Cost |
|----------|------|-------|-------------|
| Web servers | t3.medium | 3 | $X |
| Database | RDS db.r5.large | 1 | $X |
| **Total** | | | **$X** |

## Optimization Opportunities

| Opportunity | Estimated Savings | Effort |
|-------------|------------------|--------|
| Right-size web servers | $X/mo | Low |

## Cost Projection at Scale
| Scale | Monthly Cost | Notes |
|-------|-------------|-------|
| Current | $X | |
| 2x traffic | $X | Add one web server |
```

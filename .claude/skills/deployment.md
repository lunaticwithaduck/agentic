---
name: deployment
description: Provide deployment guidance with checklists and rollback strategies
activation:
  keywords: ["deploy", "deployment", "release", "rollback", "ship", "go live", "production"]
  file_patterns: ["**/deploy/**", "**/deployment/**", "**/k8s/**", "**/terraform/**"]
---

# Deployment Guide

## Purpose
Create deployment plans with checklists, environment configuration, rollback strategies, and monitoring for any deployment target.

## Instructions

1. **Identify the deployment target**:
   - Cloud platforms (AWS, GCP, Azure)
   - Container orchestration (Kubernetes, ECS, Docker Swarm)
   - Platform as a Service (Heroku, Railway, Fly.io, Render)
   - Serverless (Lambda, Cloud Functions, Vercel, Netlify)
   - VPS / bare metal (manual or Ansible/Chef)
   - Static hosting (S3, CloudFront, GitHub Pages)

2. **Create pre-deployment checklist**:
   - All tests passing on the target branch
   - Database migrations tested and reversible
   - Environment variables configured for target environment
   - Secrets rotated if needed
   - Dependencies audited for vulnerabilities
   - Performance tested under expected load
   - Rollback plan documented and tested
   - Team notified of deployment window

3. **Environment configuration**:
   - Document every required environment variable
   - Separate config by environment (dev/staging/prod)
   - Use .env.example as a template (never commit real values)
   - Validate config at application startup

4. **Database migrations**:
   - Run migrations before deploying new code
   - Ensure backward compatibility during rolling deploys
   - Test rollback of each migration
   - Back up database before destructive migrations

5. **Deployment strategy**:
   - **Rolling**: Gradually replace instances (good default)
   - **Blue/green**: Run both versions, switch traffic (zero downtime)
   - **Canary**: Route small percentage to new version first
   - Choose based on risk tolerance and infrastructure

6. **Post-deployment**:
   - Verify health check endpoints respond
   - Check error rates and latency in monitoring
   - Run smoke tests against production
   - Monitor for 15-30 minutes before considering stable
   - Communicate deployment completion to the team

7. **Rollback plan**:
   - Document exact rollback steps
   - Keep previous version artifacts available
   - Test rollback procedure periodically
   - Define rollback triggers (error rate > X%, latency > Yms)

## Output Format

A deployment document with numbered checklist items, environment variable table, step-by-step deploy commands, and rollback procedure.

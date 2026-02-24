---
name: secrets-management
description: Audit and guide secrets management practices for the project
activation:
  keywords: ["secrets", "api keys", "credentials", "secret management", "vault", "env vars", "hardcoded passwords"]
  file_patterns: ["**/.env*", "**/.gitignore", "**/docker-compose*", "**/credentials*"]
---

# Secrets Management

## Purpose
Identify all secrets in the project, ensure none are hardcoded or committed,
and recommend a robust secrets management approach.

## Instructions

1. **Identify All Secrets**
   - Search for API keys, tokens, passwords, connection strings
   - Check common patterns: `API_KEY`, `SECRET`, `PASSWORD`, `TOKEN`, `DSN`
   - Review config files, environment files, docker-compose files
   - Check CI/CD configuration files for embedded secrets
   - Look in test fixtures and seed data for real credentials

2. **Verify No Hardcoded Secrets**
   - Grep for string literals matching secret patterns (base64 tokens, UUIDs, long hex strings)
   - Check for private keys (RSA, SSH, PGP) in the repository
   - Review `.env` files are listed in `.gitignore`
   - Ensure `.env.example` exists with placeholder values only

3. **Audit Git History**
   - Check if secrets were ever committed (even if later removed)
   - Use `git log --all -p -S 'PASSWORD'` patterns to find historical leaks
   - If found, recommend git-filter-repo or BFG Repo-Cleaner
   - Note that leaked secrets must be rotated regardless

4. **Recommend Secrets Management Approach**
   - **Development**: `.env` files with `.env.example` template
   - **CI/CD**: Platform secret stores (GitHub Secrets, GitLab CI Variables)
   - **Production**: Recommend based on infrastructure:
     - Cloud: AWS Secrets Manager, GCP Secret Manager, Azure Key Vault
     - Self-hosted: HashiCorp Vault, Doppler, SOPS
   - Always prefer injection over file-based secrets

5. **Set Up Protections**
   - Verify `.gitignore` covers: `.env`, `*.pem`, `*.key`, `credentials.*`
   - Recommend pre-commit hooks (e.g., detect-secrets, gitleaks)
   - Suggest git-secrets or similar tools for commit-time scanning

6. **Document Rotation Procedures**
   - List each secret and its rotation method
   - Define rotation schedule (90 days recommended minimum)
   - Document who has access to which secrets
   - Create runbook for emergency rotation after a leak

## Output Format

```
# Secrets Management Report

## Secrets Inventory
| Secret | Location | Type | Status |
|--------|----------|------|--------|
| DB_PASSWORD | .env | Database | OK - env var |
| API_KEY | src/config.js:12 | API Key | HARDCODED - fix required |

## Issues Found
- List of problems with severity and fix instructions

## Recommendations
1. Numbered list of actions to take, ordered by priority

## Rotation Schedule
| Secret | Last Rotated | Next Rotation | Owner |
|--------|-------------|---------------|-------|
```

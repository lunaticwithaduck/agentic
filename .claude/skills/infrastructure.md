---
name: infrastructure
description: Infrastructure as Code guidance using Terraform, Pulumi, or CloudFormation
activation:
  keywords: ["infrastructure", "iac", "terraform", "pulumi", "cloudformation", "infra as code"]
  file_patterns: ["**/*.tf", "**/pulumi/**", "**/cloudformation/**", "**/infrastructure/**"]
---

# Infrastructure as Code

## Purpose
Guide the design and implementation of infrastructure using code, following best practices for modularity, state management, and environment isolation.

## Instructions

1. **Choose the IaC tool** (if not already decided):
   - **Terraform**: Multi-cloud, large ecosystem, HCL syntax. Best for multi-provider setups.
   - **Pulumi**: Use general-purpose languages (TS, Python, Go). Best when team prefers real code.
   - **CloudFormation**: AWS-native, deep integration. Best for AWS-only shops.
   - **CDK**: AWS CloudFormation with real programming languages. Bridges CF and Pulumi.
   - Match the team's existing skills and infrastructure.

2. **Structure the project**:
   - Organize by component or service, not by resource type
   - Use modules/components for reusable infrastructure patterns
   - Separate environment config from infrastructure definition
   - Example structure:
     ```
     infrastructure/
       modules/
         networking/
         compute/
         database/
       environments/
         dev.tfvars
         staging.tfvars
         prod.tfvars
       main.tf
       variables.tf
       outputs.tf
     ```

3. **State management**:
   - Use remote state storage (S3+DynamoDB, GCS, Terraform Cloud)
   - Enable state locking to prevent concurrent modifications
   - Never commit state files to version control
   - Use separate state files per environment
   - Enable state encryption at rest

4. **Handle secrets**:
   - Never store secrets in IaC code or tfvars files
   - Use a secrets manager (AWS Secrets Manager, Vault, SOPS)
   - Reference secrets by ARN/path, not value
   - Mark sensitive outputs as sensitive

5. **Environment management**:
   - Use the same modules for all environments (dev/staging/prod)
   - Vary only through input variables (instance size, count, etc.)
   - Production should match staging as closely as possible
   - Use workspace or directory-based environment separation

6. **Best practices**:
   - Follow DRY: extract repeated patterns into modules
   - Pin provider and module versions
   - Tag all resources with: environment, service, owner, managed-by
   - Plan before apply; review plan output in CI
   - Implement drift detection on a schedule
   - Plan for disaster recovery: document restore procedures

## Output Format

Provide IaC code files with inline comments, a directory structure recommendation, and deployment instructions for each environment.

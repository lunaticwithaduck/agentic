---
name: dockerfile
description: Create or optimize Dockerfiles with best practices
activation:
  keywords: ["dockerfile", "docker", "container", "containerize", "docker image", "docker build"]
  file_patterns: ["**/Dockerfile", "**/Dockerfile.*", "**/.dockerignore", "**/docker-compose*.yml"]
---

# Dockerfile Creator/Optimizer

## Purpose
Create production-ready Dockerfiles or optimize existing ones following security, performance, and size best practices.

## Instructions

1. **Analyze the application**:
   - Identify the language/runtime and version
   - Find the dependency manifest (package.json, requirements.txt, go.mod, etc.)
   - Identify the build step (if any) and the runtime entry point
   - Check for static assets, config files, or data files needed at runtime

2. **Choose the base image**:
   - Use official images from Docker Hub
   - Prefer slim or alpine variants for smaller image size
   - Pin to a specific version tag, never use `latest`
   - For compiled languages, use multi-stage builds with a minimal runtime image

3. **Structure the Dockerfile**:
   - **Stage 1 (build)**: Install build tools, copy dependency manifest, install dependencies, copy source, build
   - **Stage 2 (runtime)**: Copy only built artifacts, set runtime config
   - Order instructions by change frequency (least -> most) to maximize layer cache:
     1. Base image and system packages
     2. Dependency manifest and install
     3. Source code copy and build
     4. Runtime configuration

4. **Security**:
   - Create and use a non-root user (`USER appuser`)
   - Do not store secrets in the image (use runtime env vars or secrets)
   - Remove build tools and caches in the same layer they are used
   - Use `COPY` instead of `ADD` unless extracting archives

5. **Production readiness**:
   - Add a `HEALTHCHECK` instruction
   - Use `tini` or `dumb-init` as PID 1 for proper signal handling
   - Set appropriate `EXPOSE` ports
   - Use `.dockerignore` to exclude: .git, node_modules, .env, tests, docs
   - Add meaningful `LABEL` metadata (maintainer, version)

6. **Optimize image size**:
   - Combine related `RUN` commands with `&&` to reduce layers
   - Clean package manager caches in the same layer (`rm -rf /var/cache/apt/*`)
   - Use `--no-install-recommends` for apt
   - Remove unnecessary files after build

## Output Format

Provide the complete Dockerfile with inline comments explaining key decisions, plus a `.dockerignore` file if one does not exist. Include a build and run command example.

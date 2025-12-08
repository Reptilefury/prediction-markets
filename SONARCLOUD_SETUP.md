# SonarCloud Setup Guide

## Current Status
The SonarCloud integration is configured but requires setup in the SonarCloud web interface.

## Error Resolution
The error "Could not find a default branch for project with key 'prediction-markets'" occurs because:
1. The project doesn't exist in SonarCloud yet
2. The organization needs to be configured

## Setup Steps

### 1. Create SonarCloud Account
1. Go to [SonarCloud.io](https://sonarcloud.io)
2. Sign in with GitHub account
3. Import your GitHub organization

### 2. Create Project
1. Click "+" → "Analyze new project"
2. Select your repository
3. Set up the project with key: `prediction-markets`

### 3. Configure GitHub Secrets
Add these secrets to your GitHub repository:
- `SONAR_TOKEN`: Get from SonarCloud → My Account → Security → Generate Token

### 4. Update Configuration
Update `sonar-project.properties`:
```properties
sonar.organization=your-actual-org-name
```

## Local Analysis
For local development without SonarCloud:
```bash
./run-sonar-local.sh
```

## Temporary Workaround
The GitHub workflow is configured to:
- Skip SonarCloud if `SONAR_TOKEN` is not set
- Continue on error to not block the build
- Use the updated `sonarqube-scan-action@v5.0.0`

## Alternative: Disable SonarCloud
To completely disable SonarCloud analysis, remove the SonarCloud step from `.github/workflows/feature-branch.yml`.

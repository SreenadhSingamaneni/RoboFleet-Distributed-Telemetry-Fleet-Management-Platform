# AWS deployment guide

The Terraform directory is a production-reference topology, not a free-tier quick start. It provisions billable MSK, NAT Gateway, Multi-AZ RDS, ElastiCache, ECS, ALB, ECR, Secrets Manager, and logging resources.

## Prerequisites

- AWS account and permission to create the documented resources
- Terraform 1.10+
- AWS CLI v2
- Docker
- a Route 53/domain plan and validated ACM certificate
- an encrypted S3 Terraform state bucket
- GitHub OIDC provider and a least-privilege deployment role

## State backend

Copy `backend.hcl.example` outside source control, fill in the bucket/key/region, and initialize:

```bash
terraform init -backend-config=backend.hcl
```

The S3 backend uses native lock files. Apply bucket versioning, encryption, access logging, and restrictive IAM in the state-account baseline.

## Bootstrap order

ECS services reference ECR images, while the deployment workflow expects ECR repositories and secrets to exist. Use this two-phase bootstrap:

1. Temporarily set service desired counts to zero.
2. Apply networking, data services, ECR, IAM, and load balancer resources.
3. Log in to ECR and push `bootstrap` images.
4. Apply again with desired backend/dashboard counts.
5. Configure the GitHub environment variables/secrets.
6. Use the deployment workflow for commit-SHA revisions.

Example planning commands:

```bash
cp terraform.tfvars.example terraform.tfvars
terraform fmt -check
terraform validate
terraform plan -out=tfplan
terraform show tfplan
terraform apply tfplan
```

Review the plan. Do not run `apply -auto-approve` from a developer laptop for production.

## GitHub configuration

Create GitHub environments named `dev`, `staging`, and `production` as needed.

- secret `AWS_DEPLOY_ROLE_ARN`: OIDC-assumable deployment role
- variable `AWS_REGION`: region matching Terraform
- production environment: required reviewers and restricted branches

The role trust policy must restrict repository, branch/environment, and audience claims. Do not store AWS access keys in GitHub.

## Authentication caveat

The supplied backend implements an API key to keep the local demo self-contained. The deploy workflow embeds that key in the dashboard bundle, which means it is not confidential from a browser user. Before calling an internet-facing environment production, replace this with Cognito or enterprise OIDC and API role authorization.

## Kafka security caveat

The reference cluster encrypts Kafka traffic with TLS but uses unauthenticated clients inside restricted security groups. Production device/backend identity should use MSK IAM/SASL or mutual TLS. The application and simulator Kafka client configuration must be updated together.

## High availability and cost decisions

- Three private AZs support three MSK brokers and ECS distribution.
- One NAT Gateway controls portfolio cost but is not AZ-independent. Production can use a NAT per AZ and VPC endpoints for ECR, S3, CloudWatch, Secrets Manager, and STS.
- RDS deletion has Terraform `prevent_destroy` plus AWS deletion protection. A deliberate destruction requires reviewed code changes and a final snapshot plan.
- The simulator desired count defaults to zero. Run it only for demos or load tests.
- Size instances from measured load; defaults are reference starting points.

## Database migrations

The backend runs Flyway during startup. For tightly controlled production releases, run migrations as a one-off ECS task before shifting traffic, especially when a schema change is not backward compatible. Follow expand/migrate/contract deployment sequencing.

## DNS

Create an alias record from the fleet domain to `load_balancer_dns_name`. `public_base_url` must exactly match the HTTPS origin so backend CORS and WebSocket handshake checks allow it.

## Observability

ECS and MSK logs go to CloudWatch. For a production monitoring stack, send Micrometer metrics through an OpenTelemetry Collector or Prometheus agent to Amazon Managed Service for Prometheus and use Amazon Managed Grafana or the organization's central platform.

Create alarms for:

- ALB 5xx and unhealthy targets
- ECS CPU/memory and deployment failures
- MSK consumer lag and under-replicated partitions
- RDS CPU, free storage, connections, and replica/failover events
- ElastiCache evictions, memory, and replication health
- outbox oldest-unpublished age

## Destruction

Data resources are protected. Never disable deletion protection merely to make a command pass. Confirm backups, retention approval, affected environment, state workspace, and recovery path before any destructive change.


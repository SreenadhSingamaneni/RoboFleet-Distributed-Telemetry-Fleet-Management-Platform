# Security policy

This portfolio project uses a configurable API key only for local demonstration. Do not treat that mechanism as end-user identity. A production deployment should terminate TLS at the load balancer, use Amazon Cognito or an enterprise OIDC provider, authorize operator roles, rotate secrets through AWS Secrets Manager, and place PostgreSQL, Redis, and Kafka in private subnets.

Report vulnerabilities privately to the repository owner. Do not include credentials, production data, or exploitable details in a public issue.


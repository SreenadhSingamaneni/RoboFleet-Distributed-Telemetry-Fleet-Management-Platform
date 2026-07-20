output "load_balancer_dns_name" {
  value = aws_lb.main.dns_name
}

output "backend_ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "dashboard_ecr_repository_url" {
  value = aws_ecr_repository.dashboard.repository_url
}

output "simulator_ecr_repository_url" {
  value = aws_ecr_repository.simulator.repository_url
}

output "api_key_secret_arn" {
  value = aws_secretsmanager_secret.api_key.arn
}

output "database_endpoint" {
  value     = aws_db_instance.postgres.address
  sensitive = true
}

output "kafka_bootstrap_brokers_tls" {
  value     = aws_msk_cluster.main.bootstrap_brokers_tls
  sensitive = true
}


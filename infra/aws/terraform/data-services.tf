resource "random_password" "database" {
  length  = 32
  special = true
}

resource "random_password" "redis" {
  length  = 32
  special = false
}

resource "random_password" "api_key" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "database_password" {
  name                    = "${local.name}/database-password"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "database_password" {
  secret_id     = aws_secretsmanager_secret.database_password.id
  secret_string = random_password.database.result
}

resource "aws_secretsmanager_secret" "redis_password" {
  name                    = "${local.name}/redis-password"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "redis_password" {
  secret_id     = aws_secretsmanager_secret.redis_password.id
  secret_string = random_password.redis.result
}

resource "aws_secretsmanager_secret" "api_key" {
  name                    = "${local.name}/api-key"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "api_key" {
  secret_id     = aws_secretsmanager_secret.api_key.id
  secret_string = random_password.api_key.result
}

resource "aws_db_subnet_group" "main" {
  name       = local.name
  subnet_ids = values(aws_subnet.private)[*].id
}

resource "aws_db_instance" "postgres" {
  identifier = "${local.name}-postgres"

  engine                      = "postgres"
  engine_version              = "17"
  instance_class              = var.database_instance_class
  allocated_storage           = 50
  max_allocated_storage       = 500
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = "fleet"
  username                    = "fleet_admin"
  password                    = random_password.database.result
  port                        = 5432
  db_subnet_group_name        = aws_db_subnet_group.main.name
  vpc_security_group_ids      = [aws_security_group.database.id]
  multi_az                    = true
  publicly_accessible         = false
  backup_retention_period     = 14
  backup_window               = "03:00-04:00"
  maintenance_window          = "sun:04:30-sun:05:30"
  auto_minor_version_upgrade  = true
  performance_insights_enabled = true
  monitoring_interval         = 0
  deletion_protection         = var.deletion_protection
  skip_final_snapshot         = !var.deletion_protection
  final_snapshot_identifier   = var.deletion_protection ? "${local.name}-final" : null
  apply_immediately            = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_elasticache_subnet_group" "main" {
  name       = local.name
  subnet_ids = values(aws_subnet.private)[*].id
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name}-redis"
  description          = "Latest robot telemetry cache"
  engine               = "redis"
  node_type            = "cache.t4g.small"
  port                 = 6379
  num_cache_clusters   = 2

  automatic_failover_enabled = true
  multi_az_enabled           = true
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = random_password.redis.result
  subnet_group_name          = aws_elasticache_subnet_group.main.name
  security_group_ids         = [aws_security_group.cache.id]
  snapshot_retention_limit   = 7
  apply_immediately          = false
}

resource "aws_msk_configuration" "main" {
  name              = "${local.name}-kafka"
  kafka_versions    = ["3.9.x"]
  server_properties = <<-PROPERTIES
    auto.create.topics.enable=false
    default.replication.factor=3
    min.insync.replicas=2
    num.partitions=12
    unclean.leader.election.enable=false
  PROPERTIES
}

resource "aws_msk_cluster" "main" {
  cluster_name           = "${local.name}-kafka"
  kafka_version          = "3.9.x"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type  = "kafka.m7g.large"
    client_subnets = values(aws_subnet.private)[*].id
    security_groups = [aws_security_group.kafka.id]
    storage_info {
      ebs_storage_info { volume_size = 100 }
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.main.arn
    revision = aws_msk_configuration.main.latest_revision
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  client_authentication {
    unauthenticated = true
  }

  enhanced_monitoring = "PER_TOPIC_PER_BROKER"

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.kafka.name
      }
    }
  }
}

resource "aws_cloudwatch_log_group" "kafka" {
  name              = "/aws/msk/${local.name}"
  retention_in_days = 30
}

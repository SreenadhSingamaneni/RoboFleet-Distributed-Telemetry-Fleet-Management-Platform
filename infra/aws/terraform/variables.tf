variable "aws_region" {
  description = "AWS region for the fleet platform."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "environment must be dev, staging, or production."
  }
}

variable "vpc_cidr" {
  type    = string
  default = "10.42.0.0/16"
}

variable "acm_certificate_arn" {
  description = "Validated ACM certificate used by the public HTTPS listener."
  type        = string
}

variable "public_base_url" {
  description = "Public dashboard origin, for example https://fleet.example.com."
  type        = string
}

variable "backend_image_tag" {
  type    = string
  default = "latest"
}

variable "dashboard_image_tag" {
  type    = string
  default = "latest"
}

variable "simulator_image_tag" {
  type    = string
  default = "latest"
}

variable "backend_desired_count" {
  type    = number
  default = 2
}

variable "dashboard_desired_count" {
  type    = number
  default = 2
}

variable "simulator_desired_count" {
  description = "Keep zero outside demos/load tests."
  type        = number
  default     = 0
}

variable "database_instance_class" {
  type    = string
  default = "db.t4g.medium"
}

variable "deletion_protection" {
  type    = bool
  default = true
}


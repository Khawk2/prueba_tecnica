# =========================
# AWS CONFIG
# =========================
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

# =========================
# PROJECT CONFIG
# =========================
variable "project_name" {
  description = "Project name for all resources"
  type        = string
  default     = "franchises-api"
}

variable "environment" {
  description = "Deployment environment"
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod."
  }

  default = "development"
}

# =========================
# DATABASE (MONGODB ATLAS)
# =========================
variable "mongodb_uri" {
  description = "MongoDB Atlas connection string"
  type        = string
  sensitive   = true
}

variable "db_name" {
  description = "Database name in MongoDB Atlas"
  type        = string
  default     = "franchises"
}

# =========================
# SECURITY (JWT)
# =========================
variable "jwt_secret" {
  description = "JWT secret key"
  type        = string
  sensitive   = true
}

# =========================
# INFRASTRUCTURE (OPTIONAL)
# =========================
variable "certificate_arn" {
  description = "ACM certificate ARN for HTTPS (optional)"
  type        = string
  default     = ""
}

variable "alert_email" {
  description = "Email for CloudWatch alerts"
  type        = string
  default     = "admin@example.com"
}

# =========================
# REDIS (OPTIONAL / ELASTICACHE)
# =========================
variable "redis_host" {
  description = "Redis host endpoint"
  type        = string
  default     = "localhost"
}

variable "redis_port" {
  description = "Redis port"
  type        = number
  default     = 6379
}

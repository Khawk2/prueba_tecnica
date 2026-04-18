# ============================================================================
# OUTPUTS - SIMPLIFIED VERSION
# ============================================================================

output "app_url" {
  description = "Application URL - Access your API here"
  value       = "http://${aws_lb.main.dns_name}"
}

output "alb_dns_name" {
  description = "DNS name of the load balancer (raw)"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "URL of the ECR repository"
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "Name of the ECS service"
  value       = aws_ecs_service.app.name
}

output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of public subnets"
  value       = aws_subnet.public[*].id
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group for ECS"
  value       = aws_cloudwatch_log_group.ecs.name
}

output "mongodb_uri_secret_arn" {
  description = "ARN of the MongoDB URI secret in Secrets Manager"
  value       = data.aws_secretsmanager_secret.mongodb_uri.arn
}

output "app_config_secret_arn" {
  description = "ARN of the application config secret"
  value       = aws_secretsmanager_secret.app_config.arn
}

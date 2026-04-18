# ============================================================================
# AWS SECRETS MANAGER - SENIOR ARCHITECTURE NOTES
# ============================================================================
# DESIGN DECISIONS:
# 1. MongoDB URI se referencia como data source (NO se crea con Terraform)
#    - El secreto se crea MANUALMENTE primero por seguridad
#    - Esto evita que secrets aparezcan en terraform.tfstate
#    - En producción con CI/CD, se inyecta via AWS CLI o Console
#
# 2. App Config centraliza configuración no-sensible
#    - JWT_SECRET se pasa como variable (NO hardcodeado aquí)
#    - Facilita rotación de secrets sin redeploy
#
# 3. Naming convention: {project}/{environment}/{secret-name}
#    - Permite multi-environment con mismo código
#    - Facilita IAM policies por environment
# ============================================================================

# ---------------------------------------------------------------------------
# MONGODB URI - DATA SOURCE (Secret creado manualmente)
# ---------------------------------------------------------------------------
# WHY: MongoDB Atlas es externo a AWS. La URI contiene credenciales que NO
# deben estar en código. El equipo de seguridad crea este secret manualmente
# en AWS Console antes del primer deploy.
# ---------------------------------------------------------------------------
data "aws_secretsmanager_secret" "mongodb_uri" {
  name = "${var.project_name}/mongodb-uri"
}

data "aws_secretsmanager_secret_version" "mongodb_uri" {
  secret_id = data.aws_secretsmanager_secret.mongodb_uri.id
}

# ---------------------------------------------------------------------------
# APP CONFIG SECRET - Centralización de configuración
# ---------------------------------------------------------------------------
# WHY: Centralizar config permite cambiar valores sin redeploy de infra.
# El JWT_SECRET se pasa via variable de entorno del pipeline CI/CD,
# NO hardcodeado en este archivo.
# ---------------------------------------------------------------------------
resource "aws_secretsmanager_secret" "app_config" {
  name                    = "${var.project_name}/${var.environment}/app-config"
  description             = "Application configuration for ${var.project_name} ${var.environment}"
  recovery_window_in_days = 7  # Permite recuperar si se borra accidentalmente

  tags = {
    Name        = "${var.project_name}-app-config"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# NOTE: El JWT_SECRET se inyecta via variable 'jwt_secret' del pipeline CI/CD
# NO hardcodeado en código por seguridad. Ver variables.tf
resource "aws_secretsmanager_secret_version" "app_config" {
  secret_id = aws_secretsmanager_secret.app_config.id

  secret_string = jsonencode({
    # JWT_SECRET viene de variable de entorno TF_VAR_jwt_secret
    # Inyectado por CI/CD, NO hardcodeado en repositorio
    JWT_SECRET             = var.jwt_secret
    JWT_EXPIRATION         = 900000        # 15 minutos (ms)
    JWT_REFRESH_EXPIRATION = 604800000     # 7 días (ms)
    REDIS_HOST             = "localhost"     # Sin Redis en Free Tier
    REDIS_PORT             = 6379
    SERVER_PORT            = 8080
  })
}

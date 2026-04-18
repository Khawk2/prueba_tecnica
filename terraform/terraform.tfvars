# ============================================================================
# VARIABLES DE DESPLIEGUE - AJUSTAR SEGÚN TU ENTORNO
# ============================================================================

aws_region   = "us-east-1"
project_name = "franchises-api"
environment  = "dev"

# Base de Datos MongoDB Atlas (REQUERIDO)
mongodb_uri  = "mongodb+srv://ksalapeg:270700@franchises.jgetigy.mongodb.net/franchises?retryWrites=true&w=majority"
db_name     = "franchises"

# JWT Secret (REQUERIDO - Generar uno nuevo)
jwt_secret   = "cHJ1ZWJhIFRlY25pY2EgQWNjZW50dXJlIDE4MDQyMDI2"

# Configuración Opcional
certificate_arn = ""  # Dejar vacío para HTTP (solo desarrollo)
alert_email     = "admin@example.com"

# Redis (opcional - no usado en Free Tier)
redis_host = "localhost"
redis_port = 6379

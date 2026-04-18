terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend local para desarrollo - cambiar a S3 en producción
  backend "local" {
    path = "./terraform.tfstate"
  }
  
  # Para producción, descomentar y configurar:
  # backend "s3" {
  #   bucket         = "franchises-terraform-state"  # Crear primero
  #   key            = "infrastructure/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "franchises-terraform-locks"  # Crear primero
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

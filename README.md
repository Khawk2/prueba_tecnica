# API de Franquicias - Prueba Técnica Backend Developer

> **API reactiva de gestión de franquicias construida con Spring WebFlux, MongoDB y Arquitectura Limpia.**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](./)
[![Test Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)](./)
[![Java](https://img.shields.io/badge/java-17-blue)](./)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2.4-green)](./)

---

## Resumen del Proyecto

API reactiva de gestión de franquicias desarrollada con Spring WebFlux, MongoDB y Arquitectura Limpia. Demuestra habilidades de desarrollo backend incluyendo programación reactiva, diseño guiado por dominio, contenerización e infrastructure as code.

### Despliegue en AWS

La aplicación se encuentra desplegada en **AWS ECS Fargate** con las siguientes características:

| Recurso | Detalle |
|---------|--------|
| **Compute** | AWS ECS Fargate (0.25 vCPU, 512 MB RAM) |
| **Balanceador** | Application Load Balancer (HTTP puerto 80) |
| **Base de Datos** | MongoDB Atlas (cloud) |
| **Región** | us-east-1 |
| **Contenedores** | AWS ECR + Docker |
| **Secretos** | AWS Secrets Manager |
| **Logs** | AWS CloudWatch Logs |
| **Auto Scaling** | 1-2 tareas (CPU/Memory target 80%) |

**URL de la API desplegada:**

```
http://franchises-api-1585613032.us-east-1.elb.amazonaws.com
```

**Endpoints principales en producción:**
- Health Check: `http://franchises-api-1585613032.us-east-1.elb.amazonaws.com/actuator/health`
- API Base: `http://franchises-api-1585613032.us-east-1.elb.amazonaws.com/api/v1`
- Franquicias: `http://franchises-api-1585613032.us-east-1.elb.amazonaws.com/api/v1/franchises`

---

## Stack Tecnológico

| Capa | Tecnología |
|------|------------|
| **Framework** | Spring Boot 3.2.4 + WebFlux |
| **Base de Datos** | MongoDB 6.0 + Reactive Driver |
| **Cache** | Redis (Rate Limiting) |
| **Seguridad** | Autenticación JWT |
| **Resiliencia** | Circuit Breaker con Resilience4j |
| **Testing** | JUnit 5, Mockito, Testcontainers |
| **Contenedor** | Docker + Docker Compose |
| **IaC** | Terraform (AWS) |
| **Build** | Maven 3.8+ |

---

## Arquitectura General

Arquitectura Limpia con programación reactiva y patrón CQRS:

```
API Gateway
    |
    v
Rate Limiter (Redis)
    |
    v
Autenticación (JWT)
    |
    v
Aplicación Spring WebFlux
    |
    |--> Capa Dominio (Entidades, Reglas de Negocio)
    |
    |--> Capa Aplicación (Casos de Uso, DTOs)
    |
    |--> Capa Infraestructura (Controladores, Repositorios)
    |
    v
MongoDB (Principal) + Redis (Cache)
```

**Decisiones de Diseño Clave:**
- **Patrón CQRS**: Modelos de escritura/lectura separados para rendimiento óptimo
- **Programación Reactiva**: I/O no bloqueante con WebFlux
- **Arquitectura Limpia**: Diseño guiado por dominio con clara separación de capas
- **Consistencia Eventual**: Los productos referencian sucursales pero existen independientemente

---

## Inicio Rápido

```bash
# Opción 1: Desarrollo Local (Recomendado)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Opción 2: Docker
docker-compose up -d

# Probar endpoints
./scripts/test-endpoints.sh
```

API disponible en: `http://localhost:8080`

**Producción (AWS):** `http://franchises-api-1585613032.us-east-1.elb.amazonaws.com`

---

## Endpoints de la API

### Franquicias
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/franchises` | Crear franquicia |
| `GET` | `/api/v1/franchises` | Listar franquicias |
| `GET` | `/api/v1/franchises/{id}` | Obtener franquicia por ID |
| `PATCH` | `/api/v1/franchises/{id}/name` | Actualizar nombre de franquicia |
| `DELETE` | `/api/v1/franchises/{id}` | Eliminar franquicia |

### Sucursales
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/franchises/{fid}/branches` | Crear sucursal |
| `GET` | `/api/v1/franchises/{fid}/branches` | Listar sucursales |
| `PATCH` | `/api/v1/franchises/{fid}/branches/{bid}/name` | Actualizar nombre de sucursal |
| `DELETE` | `/api/v1/franchises/{fid}/branches/{bid}` | Eliminar sucursal |

### Productos
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v1/franchises/{fid}/branches/{bid}/products` | Crear producto |
| `GET` | `/api/v1/products/{id}` | Obtener producto por ID |
| `PATCH` | `/api/v1/products/{id}/stock` | Actualizar stock de producto |
| `PATCH` | `/api/v1/products/{id}/name` | Actualizar nombre de producto |
| `DELETE` | `/api/v1/products/{id}` | Eliminar producto |

### Consultas Especiales
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/v1/franchises/{id}/top-products` | Productos con más stock por sucursal |

---

## Cómo Ejecutar

### Desarrollo Local

**Requisitos:**
- Java 17+
- Maven 3.8+
- MongoDB local

**Pasos:**
```bash
# 1. Instalar MongoDB localmente (ver README-LOCAL.md)

# 2. Ejecutar con perfil local
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. Probar endpoints
./scripts/test-endpoints.sh
```

### Docker

**Requisitos:**
- Docker & Docker Compose

**Pasos:**
```bash
# 1. Iniciar infraestructura
docker-compose up -d

# 2. Construir y ejecutar
docker-compose up -d app

# 3. Probar
./scripts/test-endpoints.sh
```

---

## Testing

### Pruebas Automatizadas de Endpoints
```bash
# PowerShell (Windows)
.\scripts\test-endpoints.ps1

# Bash (Linux/macOS)
./scripts/test-endpoints.sh
```

**Pruebas Cubiertas:**
- Health checks
- Flujo de autenticación
- Operaciones CRUD para todas las entidades
- Consulta de productos top
- Limpieza de datos

### Tests Unitarios
```bash
mvn clean test
```

### Tests de Integración
```bash
mvn clean test -Pintegration
```

### Reporte de Cobertura
```bash
mvn clean test jacoco:report
# Reporte en: target/site/jacoco/index.html
```

---

## Decisiones de Diseño Clave

### Implementación CQRS
- **Write Model**: URLs anidadas para creación con contexto (`/franchises/{fid}/branches/{bid}/products`)
- **Read Model**: Acceso directo por ID para rendimiento (`/products/{id}`)

### Modelo de Dominio
- **Franquicia**: Aggregate root conteniendo branches como value objects
- **Producto**: Colección separada con referencia a branch para alta cardinalidad
- **Invariantes**: Las branches no pueden existir sin franquicias, los productos requieren referencia válida a branch

### Arquitectura Reactiva
- I/O no bloqueante throughout the stack
- Driver reactivo de MongoDB para operaciones de base de datos
- WebFlux para manejo de requests

---

## Infraestructura

### Despliegue AWS (Producción)

La infraestructura está desplegada en AWS usando Terraform y se encuentra activa:

| Servicio AWS | Recurso | Estado |
|-------------|---------|--------|
| **VPC** | 10.0.0.0/16 con 2 subnets públicas | Activo |
| **ECS Cluster** | franchises-api (Fargate) | Activo |
| **ALB** | franchises-api-1585613032.us-east-1.elb.amazonaws.com | Activo |
| **ECR** | Repositorio de imágenes Docker | Activo |
| **Secrets Manager** | MongoDB URI + App Config | Activo |
| **CloudWatch** | Logs y métricas del servicio | Activo |
| **S3** | Logs del ALB | Activo |
| **Auto Scaling** | CPU/Memory target tracking | Activo |

**URL de acceso:** http://franchises-api-1585613032.us-east-1.elb.amazonaws.com

### Terraform (IaC)
```bash
cd terraform
terraform init
terraform plan
terraform apply
```

**Provisiona:**
- VPC con subnets públicas (2 AZs para ALB)
- ECS Fargate para contenedores
- Application Load Balancer (HTTP)
- ECR Repository con scan on push
- Secrets Manager para credenciales
- CloudWatch Logs (retención 14 días)
- S3 Bucket para ALB access logs
- Auto Scaling (1-2 tareas por CPU/Memory)
- SNS Topic para alertas
- IAM Roles para ECS task execution y task role

### Soporte Docker
- Dockerfile multi-stage
- Docker Compose para desarrollo local
- Health checks incluidos

---

## Autor

Prueba Técnica Backend Developer - API reactiva demostrando Clean Architecture, WebFlux, MongoDB y capacidades de despliegue en la nube.

---

*Desarrollado con Spring Boot 3.2.4, Java 17 y prácticas modernas de desarrollo backend.*
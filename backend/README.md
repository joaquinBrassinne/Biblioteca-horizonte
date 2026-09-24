# Backend - Biblioteca Horizonte (MVP)

Backend desarrollado en **Java 21**, **Spring Boot 3.4.3**, **Spring Data JPA**, **Bean Validation** y soporte para **PostgreSQL** y **H2**.

---

## 1. Estructura Limpia por Capas

El proyecto está organizado siguiendo el estándar de arquitectura por capas (Layered Architecture):

```text
com.biblioteca.horizonte/
├── BibliotecaHorizonteApplication.java  # Clase principal Spring Boot
├── config/
│   └── CorsConfig.java                 # Configuración de CORS para el Frontend
├── controller/
│   ├── HealthController.java           # Endpoint GET /api/v1/health (verificación de estado y DB)
│   ├── ReservaController.java          # Controlador REST de solicitudes de reserva
│   └── EquipoController.java           # Controlador REST de consulta de equipos
├── service/
│   ├── ReservaService.java             # Interfaz de lógica de negocio de reservas
│   ├── EquipoService.java              # Interfaz de consulta de equipos
│   └── impl/
│       ├── ReservaServiceImpl.java     # Implementación preparada para el dominio (Fase 3)
│       └── EquipoServiceImpl.java      # Implementación de consulta de catálogo de equipos
├── repository/
│   ├── ReservaRepository.java          # Consultas JPA derivadas y verificación de slots
│   ├── EquipoRepository.java           # Persistencia de equipos
│   └── DocenteRepository.java          # Persistencia de docentes
├── entity/
│   ├── Reserva.java                    # Entidad JPA principal (tabla: reservas)
│   ├── Equipo.java                     # Entidad JPA (tabla: equipos)
│   ├── Docente.java                    # Entidad JPA (tabla: docentes)
│   └── EstadoReserva.java              # Enum: PENDIENTE, CONFIRMADA, RECHAZADA
├── dto/
│   ├── request/
│   │   └── CrearReservaRequest.java    # Validaciones con Bean Validation
│   └── response/
│       ├── ReservaResponse.java        # DTO de respuesta para reservas
│       ├── EquipoResponse.java         # DTO de respuesta para equipos
│       └── ErrorResponse.java          # DTO canónico de error según API.md
└── exception/
    ├── GlobalExceptionHandler.java     # Manejo global centralizado (@RestControllerAdvice)
    ├── ResourceNotFoundException.java  # 404 RECURSO_NO_ENCONTRADO
    ├── ReservaConflictException.java   # 409 RESERVA_COLISION_CONFIRMADA (RN-001)
    └── InvalidStateTransitionException.java # 409 TRANSICION_ESTADO_INVALIDA (RN-003)
```

---

## 2. Requisitos Previos

- **Java JDK 21** instalado y configurado en el sistema.
- **Docker** (opcional, para ejecutar PostgreSQL localmente).
- El proyecto incluye el **Maven Wrapper (`mvnw` / `mvnw.cmd`)**, por lo que no es necesario instalar Maven manualmente.

---

## 3. Base de Datos PostgreSQL con Docker

Para levantar la base de datos PostgreSQL requerida:

### Opción A: Usando Docker Compose (Recomendado)
Desde la carpeta `backend/`:
```bash
docker compose up -d
```

### Opción B: Usando el Dockerfile de PostgreSQL
```bash
docker build -f Dockerfile.postgres -t biblioteca-postgres .
docker run -d --name biblioteca-postgres -p 5432:5432 biblioteca-postgres
```

**Credenciales por defecto:**
- **Host:** `localhost`
- **Puerto:** `5432`
- **Base de datos:** `biblioteca_horizonte`
- **Usuario:** `postgres`
- **Contraseña:** `postgres`

---

## 4. Ejecución del Backend

### Modo Desarrollo Rápido (H2 en memoria)
Por defecto, el perfil activo es `dev`, el cual utiliza una base de datos H2 en memoria y carga datos de prueba iniciales (`data.sql`):

En Windows (PowerShell / CMD):
```powershell
.\mvnw.cmd spring-boot:run
```

En Linux / macOS:
```bash
./mvnw spring-boot:run
```

- **Consola H2:** `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:bibliotecadb`
  - Usuario: `sa`
  - Contraseña: *(vacía)*

### Modo Producción / PostgreSQL
Para conectar a la base de datos PostgreSQL (levantada vía Docker):

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

O estableciendo la variable de entorno:
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
.\mvnw.cmd spring-boot:run
```

---

## 5. Verificación de Funcionamiento

### Health Check Endpoint
Comprueba que el backend esté arriba y que la conexión a la base de datos esté activa:

```bash
curl http://localhost:8080/api/v1/health
```

Respuesta esperada (`200 OK`):
```json
{
  "database": "CONNECTED",
  "service": "Biblioteca Horizonte Backend API",
  "status": "UP",
  "timestamp": "2026-09-24T17:01:37.5008776"
}
```

---

## 6. Ejecución de Tests y Compilación

Para compilar y correr la suite de tests automatizados (contexto, API health y persistencia JPA):
```powershell
.\mvnw.cmd test
```

Para generar el paquete ejecutable JAR final:
```powershell
.\mvnw.cmd clean package
```
El archivo JAR generado se ubicará en `target/horizonte-0.0.1-SNAPSHOT.jar` y se puede ejecutar con:
```powershell
java -jar target/horizonte-0.0.1-SNAPSHOT.jar
```

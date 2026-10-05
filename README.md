# Biblioteca Horizonte — Sistema de Gestión de Solicitudes de Reserva de Equipos

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)
![React](https://img.shields.io/badge/React-18-blue.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)
![Vite](https://img.shields.io/badge/Vite-5.x-purple.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)

Sistema web desacoplado para la gestión de solicitudes de reserva de equipamiento tecnológico y audiovisual (proyectores, notebooks, carros de tablets) para el equipo docente y personal bibliotecario de la **Biblioteca Horizonte**.

El proyecto implementa una arquitectura basada en **Spring Boot 3 (REST API)** y **React + TypeScript (Vite)**, con autenticación y control de acceso basado en roles (RBAC), prevención estricta de colisiones de horarios, aislamiento de datos por docente y trazabilidad completa de auditoría.

---

## 📋 Características Principales

- 🔐 **Autenticación y Roles (RBAC):**
  - **Docente (`ROLE_DOCENTE`):** Emite solicitudes de reserva y consulta exclusivamente sus propias solicitudes (`/mis-solicitudes`).
  - **Bibliotecaria (`ROLE_BIBLIOTECARIA`):** Revisa solicitudes pendientes, confirma o rechaza reservas, consulta asignaciones efectivas (`/confirmadas`) y accede a la trazabilidad de auditoría.
- 🔄 **Ciclo de Vida de Reservas:**
  - Toda nueva solicitud se registra obligatoriamente en estado **`PENDIENTE`**.
  - Transiciones deterministas a estados terminales **`CONFIRMADA`** o **`RECHAZADA`**.
- 🛡️ **Prevención Estricta de Colisiones (Regla RN-001):**
  - Garantiza que **no pueda existir más de una reserva `CONFIRMADA`** para la misma tupla `(equipo, fecha, módulo)`.
  - Permite la coexistencia de múltiples solicitudes en estado `PENDIENTE` para el mismo slot temporal, evaluando la exclusividad en el momento de la confirmación (retornando `HTTP 409 Conflict`).
- 📜 **Trazabilidad y Auditoría (`AuditoriaReserva`):**
  - Registro inmutable de cada acción crítica (confirmación o rechazo), almacenando el usuario actuante, fecha/hora y resultado.
- 🎨 **Interfaz de Usuario Interactiva:**
  - SPA desarrollada con React + TypeScript, con pildoras de estado dinámicas (`StatusBadge`), selector rápido de perfiles para desarrollo y manejo transparente de errores HTTP.

---

## 🏛️ Arquitectura del Sistema

El proyecto sigue un diseño cliente-servidor completamente desacoplado:

```text
               +----------------------------------+
               |   Frontend (React + TS + Vite)   |
               |         http://localhost:5173    |
               +----------------+-----------------+
                                |
                   HTTP Basic / REST (JSON)
                                |
               +----------------v-----------------+
               |     Backend (Spring Boot 3)      |
               |         http://localhost:8080    |
               +----------------+-----------------+
                                |
                     Spring Data JPA / SQL
                                |
               +----------------v-----------------+
               |   Base de Datos Relacional       |
               |   (H2 dev / PostgreSQL prod)     |
               +----------------------------------+
```

### Estructura del Repositorio

```text
Biblioteca-horizonte/
├── backend/                  # Aplicación Spring Boot (Java 21)
│   ├── src/main/java/com/biblioteca/horizonte/
│   │   ├── config/           # SecurityConfig, CorsConfig
│   │   ├── controller/       # ReservaController, EquipoController, HealthController
│   │   ├── dto/              # Requests y Responses (CrearReservaRequest, ReservaResponse, etc.)
│   │   ├── entity/           # Reserva, Equipo, Docente, AuditoriaReserva, EstadoReserva
│   │   ├── exception/        # GlobalExceptionHandler, ReservaConflictException, etc.
│   │   ├── repository/       # ReservaRepository, EquipoRepository, AuditoriaReservaRepository
│   │   ├── security/         # DocenteResolver, RestAuthenticationEntryPoint, RestAccessDeniedHandler
│   │   └── service/          # ReservaService, AuditoriaService y sus implementaciones
│   └── src/main/resources/   # application-dev.properties, application-prod.properties, scripts SQL
├── frontend/                 # Aplicación SPA (React + TypeScript + Vite)
│   ├── src/
│   │   ├── api/              # apiClient.ts, reservaApi.ts
│   │   ├── components/       # Componentes de UI, reserva y autenticación
│   │   ├── context/          # AuthContext.tsx (Gestión de sesión y perfiles)
│   │   ├── pages/            # ReservasPage.tsx
│   │   └── types/            # Tipos de TypeScript (reserva.ts)
└── docs/                     # Documentación técnica completa (ARCHITECTURE, API, ADR, TEST-PLAN, etc.)
```

---

## 🔑 Credenciales de Desarrollo

Para facilitar la evaluación local, la aplicación cuenta con usuarios preconfigurados en memoria (HTTP Basic Auth):

| Usuario | Contraseña | Rol | Descripción / Legajo |
| :--- | :--- | :--- | :--- |
| `docente1` | `docente123` | `ROLE_DOCENTE` | Prof. Juan Pérez (Docente ID: 1) |
| `docente2` | `docente123` | `ROLE_DOCENTE` | Prof. María González (Docente ID: 2) |
| `bibliotecaria` | `biblio123` | `ROLE_BIBLIOTECARIA` | Lucía (Personal de Biblioteca) |

*Nota: En la interfaz del frontend se incluye un selector de cambio rápido de perfil para alternar entre docentes y la bibliotecaria con un solo clic.*

---

## 🚀 Guía de Instalación y Ejecución Local

### Requisitos Previos
- **Java JDK 21** o superior.
- **Node.js LTS** (v18 o superior) y **npm**.
- **Git**.

### 1. Clonar el repositorio
```bash
git clone https://github.com/joaquinBrassinne/Biblioteca-horizonte.git
cd Biblioteca-horizonte
```

### 2. Ejecutar el Backend (Spring Boot)
En una terminal:
```bash
cd backend
./mvnw spring-boot:run
```
*(En Windows PowerShell: `.\mvnw.cmd spring-boot:run`)*

- El backend iniciará en `http://localhost:8080/api/v1`.
- Base de Datos H2 en memoria activa. Consola H2 disponible en `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:bibliotecadb`).

### 3. Ejecutar el Frontend (React + Vite)
En otra terminal:
```bash
cd frontend
npm install
npm run dev
```

- El frontend estará disponible en `http://localhost:5173`.

---

## 🧪 Ejecución de Pruebas Automáticas

### Backend (JUnit 5 + Mockito + MockMvc)
```bash
cd backend
./mvnw test
```
*Cubre 40 casos de prueba unitarios, de integración de seguridad y de persistencia relacional.*

### Frontend (Vitest)
```bash
cd frontend
npx vitest run
```
*Cubre 15 pruebas unitarias y de integración de cliente HTTP, contratos de errores y guardas de roles.*

---

## 📄 Documentación Técnica

La documentación detallada del proyecto se encuentra almacenada en la carpeta [`docs/`](./docs):

- 📐 [**ARCHITECTURE.md**](./docs/ARCHITECTURE.md): Especificación de arquitectura, diagramas ER, diagrama de secuencia de confirmación y capas del backend.
- 🔌 [**API.md**](./docs/API.md): Contrato REST completo de endpoints, payloads de entrada/salida y catálogo de errores.
- 📑 [**ADR.md**](./docs/ADR.md): Registro de 12 Decisiones de Arquitectura de Software (ADR-001 a ADR-012).
- ⚙️ [**INTEGRATION.md**](./docs/INTEGRATION.md): Guía de integración de componentes y verificación de escenarios clave.
- 🧪 [**TEST-PLAN.md**](./docs/TEST-PLAN.md): Plan de pruebas, matriz de trazabilidad y cobertura.
- 📊 [**SSD.md**](./docs/SSD.md): Documento de Especificación del Sistema original.

---

## 📝 Licencia

Este proyecto se distribuye bajo la licencia **GNU GENERAL PUBLIC LICENSE**. Consulte el archivo [LICENSE](./LICENSE) para obtener más información.

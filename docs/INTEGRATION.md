# Guía de Integración y Ejecución Simultánea
## Proyecto: Biblioteca Horizonte - MVP Gestión de Reservas

---

### 1. Resumen de la Integración (Fase 6)

El sistema integra de forma completa y desacoplada las dos aplicaciones:

```text
Frontend (React + Vite)  <--- HTTP / JSON (Proxy :5173 o CORS :8080) --->  Backend (Spring Boot)  <--->  H2 Database
```

- **Backend:** Expone la API REST en `http://localhost:8080/api/v1`.
- **Frontend:** Corre en `http://localhost:5173` y se comunica mediante proxy en `vite.config.ts` (`/api` -> `http://localhost:8080`) o de forma directa con cabeceras CORS (`Access-Control-Allow-Origin: http://localhost:5173`).
- **Base de Datos:** H2 en memoria con carga inicial automática (`data-h2.sql`) y consola web en `http://localhost:8080/h2-console`.

---

### 2. Cómo Ejecutar Frontend y Backend Simultáneamente

Para levantar ambos servicios en tu entorno local, abrí dos terminales independientes:

#### **Terminal 1: Backend (Spring Boot)**
```powershell
# Ubicarse en el directorio backend
cd c:\Users\Usuario\Desktop\proyecto\Biblioteca-horizonte\backend

# Requisito de runtime: Java 21 LTS (Eclipse Temurin 21)
# Ejecutar el servidor con Maven Wrapper (utilizando perfil dev por defecto)
.\mvnw.cmd spring-boot:run
```
*(En Linux / macOS: `./mvnw spring-boot:run`)*

- El backend quedará escuchando en `http://localhost:8080`.
- Datos de prueba precargados:
  - Equipos:
    - ID 10: *Proyector EPSON Aula Magna*
    - ID 11: *Notebook HP Laboratorio 1*
    - ID 12: *Carro de Tablets #1*
  - Docentes:
    - ID 1: *Prof. Juan Pérez*
    - ID 2: *Prof. María González*

#### **Terminal 2: Frontend (React + Vite)**
```powershell
# Ubicarse en el directorio frontend
cd c:\Users\Usuario\Desktop\proyecto\Biblioteca-horizonte\frontend

# Si es la primera vez o se actualizaron paquetes:
npm install

# Iniciar servidor de desarrollo Vite
npm run dev
```

- El frontend quedará disponible en `http://localhost:5173` (o `http://127.0.0.1:5173`).

---

### 3. Variables de Entorno y Configuración

#### **Backend (`application.properties` / `application-dev.properties`)**
| Variable / Propiedad | Valor por defecto | Descripción |
| :--- | :--- | :--- |
| `spring.profiles.active` | `dev` | Perfil de configuración activo (`dev` para H2, `prod` para PostgreSQL). |
| `server.port` | `8080` | Puerto HTTP donde corre Tomcat embebido. |
| `spring.datasource.url` | `jdbc:h2:mem:bibliotecadb;...` | Cadena JDBC de conexión a base de datos. |

#### **Frontend (`.env` o variables de entorno Vite)**
| Variable | Valor por defecto | Descripción |
| :--- | :--- | :--- |
| `VITE_API_URL` | `/api/v1` | URL base de la API REST. Al usar `/api/v1`, Vite redirige mediante proxy a `http://localhost:8080`. Si se especifica `http://localhost:8080/api/v1`, se conecta directamente vía CORS. |

---

### 4. Verificación de los Escenarios Clave

El sistema fue verificado de extremo a extremo comprobando el flujo completo:

#### **CASO 1: Creación de Solicitud de Reserva**
- **Acción:** El docente envía una solicitud para:
  - Equipo: ID 10 (*Proyector EPSON Aula Magna*)
  - Fecha: `2026-10-25`
  - Módulo: `M1 (08:00 - 09:30)`
- **Petición HTTP:** `POST /api/v1/reservas`
- **Resultado Backend:** Código `201 Created` con payload:
  ```json
  {
    "id": 1,
    "docenteId": 1,
    "equipoId": 10,
    "equipoNombre": "Proyector EPSON Aula Magna",
    "fecha": "2026-10-25",
    "modulo": "M1",
    "estado": "PENDIENTE"
  }
  ```
- **Comportamiento en UI:**
  - Muestra la notificación: *"La solicitud #1 ha sido registrada con éxito. Su estado actual es PENDIENTE a la espera de confirmación."*
  - **No muestra** "Reserva realizada".
  - La tarjeta aparece en la lista con el badge ámbar **PENDIENTE**.

#### **CASO 2: Confirmación de Solicitud**
- **Acción:** Se hace clic en el botón *"Confirmar"* de la Solicitud #1.
- **Petición HTTP:** `POST /api/v1/reservas/1/confirmar`
- **Resultado Backend:** Código `200 OK` con `estado: "CONFIRMADA"`.
- **Comportamiento en UI:**
  - El badge de la Solicitud #1 se actualiza a verde **CONFIRMADA**.
  - Muestra el cartel: *"Reserva confirmada. Slot asegurado."*
  - Los botones de acción se ocultan al alcanzar el estado terminal.

#### **CASO 3: Intento de Confirmación con Conflicto de Slot (Regla RN-001)**
- **Acción:**
  1. Se crea una segunda solicitud para el **mismo equipo (10), misma fecha (2026-10-25) y mismo módulo (M1)** con docente ID 2.
  2. La solicitud se registra con éxito en estado inicial **PENDIENTE** (Solicitud #2).
  3. Se hace clic en el botón *"Confirmar"* de la Solicitud #2.
- **Petición HTTP:** `POST /api/v1/reservas/2/confirmar`
- **Resultado Backend:** Código `409 Conflict`:
  ```json
  {
    "timestamp": "2026-09-24T17:40:01Z",
    "status": 409,
    "error": "Conflict",
    "codigo": "RESERVA_COLISION_CONFIRMADA",
    "mensaje": "No es posible confirmar la reserva. Ya existe otra reserva confirmada para el equipo 10 en la fecha 2026-10-25 y módulo M1.",
    "path": "/api/v1/reservas/2/confirmar"
  }
  ```
- **Comportamiento en UI:**
  - La UI intercepta el código 409 y presenta un banner de error explícito:
    > **Conflicto de Reserva (RN-001):** *No es posible confirmar la reserva. Ya existe otra reserva confirmada para el equipo 10 en la fecha 2026-10-25 y módulo M1.*
  - **La Solicitud #2 NO pasa a confirmada**: Permanece en estado **PENDIENTE**.

#### **CASO 4: Rechazo de Solicitud**
- **Acción:** Se hace clic en *"Rechazar"* en la Solicitud #2.
- **Petición HTTP:** `POST /api/v1/reservas/2/rechazar`
- **Resultado Backend:** Código `200 OK` con `estado: "RECHAZADA"`.
- **Comportamiento en UI:**
  - El badge pasa a rojo **RECHAZADA**.
  - Si posteriormente se intenta invocar la confirmación sobre una solicitud rechazada, el backend responde con `409 Conflict` (`TRANSICION_ESTADO_INVALIDA`).

---

### 5. Verificación de CORS y Seguridad de Comunicación

Se verificó la respuesta ante solicitudes preflight `OPTIONS`:
```http
OPTIONS /api/v1/reservas HTTP/1.1
Origin: http://localhost:5173
Access-Control-Request-Method: POST

HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
Access-Control-Allow-Credentials: true
```
El backend autoriza explícitamente el origen del frontend para todos los métodos HTTP necesarios.

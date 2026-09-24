# Especificación de la API REST - Biblioteca Horizonte
## MVP: Gestión de Reservas

---

### 1. Convenciones Generales

- **URL Base:** `http://localhost:8080/api/v1`
- **Formato de Intercambio:** `application/json`
- **Juego de Caracteres:** `UTF-8`
- **Cabeceras Comunes:**
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Formato de Fechas:** ISO-8601 para fechas (`YYYY-MM-DD`, ej: `2026-10-15`) y timestamps (`YYYY-MM-DDTHH:mm:ssZ`).

---

### 2. Formato Estándar de Error (`ErrorResponse`)

Todas las respuestas con código HTTP 4xx o 5xx siguen una estructura homogénea:

```json
{
  "timestamp": "2026-09-24T16:35:00Z",
  "status": 409,
  "error": "Conflict",
  "codigo": "RESERVA_COLISION_CONFIRMADA",
  "mensaje": "Ya existe una reserva confirmada para el equipo 10 en la fecha 2026-10-15 y módulo M1.",
  "path": "/api/v1/reservas/101/confirmar",
  "validaciones": null
}
```

En caso de fallos de validación (`HTTP 400 Bad Request`), el atributo `validaciones` contiene un mapa de campo -> mensaje de error:

```json
{
  "timestamp": "2026-09-24T16:35:00Z",
  "status": 400,
  "error": "Bad Request",
  "codigo": "DATOS_INVALIDOS",
  "mensaje": "Uno o más campos presentan errores de validación.",
  "path": "/api/v1/reservas",
  "validaciones": {
    "docenteId": "El identificador del docente es obligatorio",
    "fecha": "La fecha de reserva es obligatoria",
    "modulo": "El módulo horario es obligatorio"
  }
}
```

---

### 3. Catálogo de Códigos de Error de Dominio

| Código de Negocio | HTTP Status | Causa |
| :--- | :--- | :--- |
| `DATOS_INVALIDOS` | 400 Bad Request | Violación de Bean Validation (campos faltantes o tipos incompatibles). |
| `RECURSO_NO_ENCONTRADO` | 404 Not Found | No existe la reserva o equipo con el identificador provisto. |
| `RESERVA_COLISION_CONFIRMADA` | 409 Conflict | Intento de confirmar una solicitud sobre un slot (equipo + fecha + módulo) que ya posee otra reserva confirmada (**RN-001**). |
| `TRANSICION_ESTADO_INVALIDA` | 409 Conflict | Intento de confirmar o rechazar una reserva que no está en estado `PENDIENTE` (**RN-003**). |
| `ERROR_INTERNO_SERVIDOR` | 500 Internal Error | Fallo imprevisto de infraestructura o base de datos. |

---

### 4. Endpoints del Recurso: Reservas (`/reservas`)

---

#### 4.1. Crear Solicitud de Reserva
Crea y registra una nueva solicitud de reserva en estado inicial `PENDIENTE`.

- **Método HTTP:** `POST`
- **Ruta:** `/api/v1/reservas`
- **Requerimientos asociados:** REQ-001, REQ-002, RN-002, CU-001

##### Parámetros de Petición
- **Body (`application/json`):**
  | Campo | Tipo | Obligatorio | Descripción |
  | :--- | :--- | :--- | :--- |
  | `docenteId` | Long | Sí | Identificador del docente solicitante. |
  | `equipoId` | Long | Sí | Identificador del equipo a reservar. |
  | `fecha` | String (LocalDate) | Sí | Fecha requerida en formato `YYYY-MM-DD`. |
  | `modulo` | String | Sí | Módulo horario solicitado (ej: `"M1"`, `"08:00-09:30"`). *[DECISIÓN PENDIENTE: Definir catálogo estandarizado]* |

##### Ejemplo de Request Body:
```json
{
  "docenteId": 1,
  "equipoId": 10,
  "fecha": "2026-10-15",
  "modulo": "M1"
}
```

##### Respuestas:
- **`201 Created`**: Solicitud registrada con éxito.
  ```json
  {
    "id": 101,
    "docenteId": 1,
    "equipoId": 10,
    "equipoNombre": "Proyector EPSON Aula Magna",
    "fecha": "2026-10-15",
    "modulo": "M1",
    "estado": "PENDIENTE",
    "fechaCreacion": "2026-09-24T16:35:00Z"
  }
  ```
- **`400 Bad Request`**: Datos faltantes o inválidos.
- **`404 Not Found`**: El `equipoId` especificado no existe en el sistema.

---

#### 4.2. Listar Solicitudes de Reserva
Obtiene la lista completa de solicitudes registradas para su visualización y monitoreo de estado.

- **Método HTTP:** `GET`
- **Ruta:** `/api/v1/reservas`
- **Requerimientos asociados:** REQ-002, CU-002

##### Parámetros de Consulta (Query Params) *[DECISIÓN PENDIENTE: Filtros opcionales para MVP]*:
- `estado` *(opcional)*: Filtrar por `PENDIENTE`, `CONFIRMADA` o `RECHAZADA`.
- `docenteId` *(opcional)*: Filtrar por identificador de docente.

##### Respuestas:
- **`200 OK`**: Lista de solicitudes (puede ser vacía `[]`).
  ```json
  [
    {
      "id": 101,
      "docenteId": 1,
      "equipoId": 10,
      "equipoNombre": "Proyector EPSON Aula Magna",
      "fecha": "2026-10-15",
      "modulo": "M1",
      "estado": "PENDIENTE",
      "fechaCreacion": "2026-09-24T16:35:00Z"
    },
    {
      "id": 102,
      "docenteId": 2,
      "equipoId": 12,
      "equipoNombre": "Carro de Tablets #1",
      "fecha": "2026-10-16",
      "modulo": "M2",
      "estado": "CONFIRMADA",
      "fechaCreacion": "2026-09-24T15:20:00Z"
    }
  ]
  ```

---

#### 4.3. Obtener Detalle de una Solicitud
Recupera los datos individuales de una solicitud específica mediante su ID.

- **Método HTTP:** `GET`
- **Ruta:** `/api/v1/reservas/{id}`
- **Requerimientos asociados:** REQ-002, CU-002

##### Parámetros de Ruta:
- `id` (Long, obligatorio): Identificador numérico de la reserva.

##### Respuestas:
- **`200 OK`**: Detalle de la reserva encontrada.
  ```json
  {
    "id": 101,
    "docenteId": 1,
    "equipoId": 10,
    "equipoNombre": "Proyector EPSON Aula Magna",
    "fecha": "2026-10-15",
    "modulo": "M1",
    "estado": "PENDIENTE",
    "fechaCreacion": "2026-09-24T16:35:00Z"
  }
  ```
- **`404 Not Found`**: No existe una reserva con el ID indicado.

---

#### 4.4. Confirmar Solicitud de Reserva
Ejecuta la confirmación de una solicitud en estado `PENDIENTE`. Aplica de forma obligatoria la validación de no superposición (**RN-001**).

- **Método HTTP:** `POST` *(o `PATCH /api/v1/reservas/{id}/confirmar`)*
- **Ruta:** `/api/v1/reservas/{id}/confirmar`
- **Requerimientos asociados:** REQ-003, REQ-005, RN-001, RN-003, CU-003, CA-003, CA-005

##### Parámetros de Ruta:
- `id` (Long, obligatorio): Identificador de la reserva a confirmar.

##### Request Body:
- No requiere body.

##### Respuestas:
- **`200 OK`**: La solicitud fue confirmada con éxito.
  ```json
  {
    "id": 101,
    "docenteId": 1,
    "equipoId": 10,
    "equipoNombre": "Proyector EPSON Aula Magna",
    "fecha": "2026-10-15",
    "modulo": "M1",
    "estado": "CONFIRMADA",
    "fechaCreacion": "2026-09-24T16:35:00Z"
  }
  ```
- **`404 Not Found`**: La reserva con el ID provisto no existe.
- **`409 Conflict` (Colisión de slot)**: Ya existe otra reserva `CONFIRMADA` para el mismo equipo, fecha y módulo (**RN-001**).
  ```json
  {
    "timestamp": "2026-09-24T16:36:00Z",
    "status": 409,
    "error": "Conflict",
    "codigo": "RESERVA_COLISION_CONFIRMADA",
    "mensaje": "No es posible confirmar la reserva. Ya existe otra reserva confirmada para el equipo 10 en la fecha 2026-10-15 y módulo M1.",
    "path": "/api/v1/reservas/101/confirmar",
    "validaciones": null
  }
  ```
- **`409 Conflict` (Transición inválida)**: La reserva no está en estado `PENDIENTE` (**RN-003**).
  ```json
  {
    "timestamp": "2026-09-24T16:36:00Z",
    "status": 409,
    "error": "Conflict",
    "codigo": "TRANSICION_ESTADO_INVALIDA",
    "mensaje": "Solo se pueden confirmar solicitudes en estado PENDIENTE. Estado actual: RECHAZADA.",
    "path": "/api/v1/reservas/101/confirmar",
    "validaciones": null
  }
  ```

---

#### 4.5. Rechazar Solicitud de Reserva
Transiciona una solicitud de reserva en estado `PENDIENTE` a estado `RECHAZADA`.

- **Método HTTP:** `POST` *(o `PATCH /api/v1/reservas/{id}/rechazar`)*
- **Ruta:** `/api/v1/reservas/{id}/rechazar`
- **Requerimientos asociados:** REQ-004, RN-003, CU-004, CA-004

##### Parámetros de Ruta:
- `id` (Long, obligatorio): Identificador de la reserva a rechazar.

##### Request Body:
- No requiere body *(o motivo opcional si se definiera en versión posterior)*.

##### Respuestas:
- **`200 OK`**: La solicitud fue rechazada con éxito.
  ```json
  {
    "id": 101,
    "docenteId": 1,
    "equipoId": 10,
    "equipoNombre": "Proyector EPSON Aula Magna",
    "fecha": "2026-10-15",
    "modulo": "M1",
    "estado": "RECHAZADA",
    "fechaCreacion": "2026-09-24T16:35:00Z"
  }
  ```
- **`404 Not Found`**: La reserva con el ID provisto no existe.
- **`409 Conflict` (Transición inválida)**: La reserva ya no se encuentra en estado `PENDIENTE` (**RN-003**).

---

### 5. Endpoints de Recursos Auxiliares: Equipos (`/equipos`)

---

#### 5.1. Listar Equipos
Permite a la aplicación frontend obtener la lista de equipos disponibles para poblar el control de selección en el formulario de solicitud.

- **Método HTTP:** `GET`
- **Ruta:** `/api/v1/equipos`

##### Respuestas:
- **`200 OK`**:
  ```json
  [
    {
      "id": 10,
      "nombre": "Proyector EPSON Aula Magna"
    },
    {
      "id": 11,
      "nombre": "Notebook HP Laboratorio 1"
    },
    {
      "id": 12,
      "nombre": "Carro de Tablets #1"
    }
  ]
  ```

---

### 6. Consideraciones de Futura Versión (Fuera de Alcance del MVP)

- `POST /api/v1/reservas/{id}/recordatorio`: Notificación por correo *(explícitamente fuera de alcance)*.
- `DELETE /api/v1/reservas/{id}` o `POST /api/v1/reservas/{id}/cancelar`: Cancelación de reservas confirmadas *(no contemplado en requerimiento MVP)*.
- `POST /api/v1/equipos`: Endpoint administrativo para dar de alta nuevos equipos *(fuera de alcance MVP)*.

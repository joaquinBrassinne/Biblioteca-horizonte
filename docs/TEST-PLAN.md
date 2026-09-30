# Test Plan
## Proyecto: Biblioteca Horizonte — MVP Gestión de Reservas

**Versión:** 1.0  
**Fecha:** 2026-09-30  
**Responsable:** Equipo de desarrollo  
**Referencias ADR:** [ADR.md](./ADR.md)  
**Referencia SSD:** SSD — MVP Gestión de Reservas (secciones 2, 5, 6, 15, 16 y 17)

---

## Índice

1. [Alcance y estrategia de pruebas](#1-alcance-y-estrategia-de-pruebas)
2. [Entorno de pruebas](#2-entorno-de-pruebas)
3. [Trazabilidad: Requisitos → Casos de Prueba](#3-trazabilidad-requisitos--casos-de-prueba)
4. [Catálogo de casos de prueba](#4-catálogo-de-casos-de-prueba)
5. [Cobertura de pruebas de seguridad](#5-cobertura-de-pruebas-de-seguridad)
6. [Pruebas de frontend](#6-pruebas-de-frontend)
7. [Brechas de cobertura y pruebas pendientes](#7-brechas-de-cobertura-y-pruebas-pendientes)
8. [Resultados de ejecución](#8-resultados-de-ejecución)
9. [Riesgos y notas](#9-riesgos-y-notas)

---

## 1. Alcance y estrategia de pruebas

### 1.1 Niveles de prueba implementados

| Nivel | Framework | Alcance | Estado |
|---|---|---|---|
| **Unitarias (Servicio)** | JUnit 5 + Mockito | `ReservaServiceImpl`: lógica de negocio y transiciones de estado aisladas de persistencia | Implementado |
| **Repositorio (@DataJpaTest)** | JUnit 5 + Spring Data + H2 | Consultas JPQL de `ReservaRepository`: persistencia, filtros por estado, equipo y fecha | Implementado |
| **Integración/MockMvc (@SpringBootTest)** | JUnit 5 + MockMvc + Spring Security Test | Flujos HTTP completos con autenticación real: creación, confirmación, rechazo, acceso denegado, auditoría | Implementado |
| **Smoke test (contexto)** | JUnit 5 + SpringBootTest | Arranque del contexto de aplicación completo con perfil `dev` | Implementado |
| **Frontend (unitarias de lógica)** | Vitest | Reglas de estados, cliente HTTP (`ApiClient`/`ApiError`), llamadas a endpoints por rol | Implementado |
| **E2E / componentes React** | — | No existe suite E2E (Cypress/Playwright); tampoco tests de componentes (RTL/Storybook) | Pendiente |

### 1.2 Estrategia de aislamiento

- **Perfil `dev`:** todos los tests de backend se ejecutan contra H2 en memoria con los archivos `schema-h2.sql` y `data-h2.sql`. La base de datos se reinicializa por sesión de Spring.
- **BeforeEach de integración:** `ReservaControllerIntegrationTest` y `AuditoriaIntegrationTest` llaman a `reservaRepository.deleteAll()` y `auditoriaReservaRepository.deleteAll()` antes de cada test, garantizando aislamiento entre casos.
- **Mockito:** `ReservaServiceTest` usa `@ExtendWith(MockitoExtension.class)` con mocks estrictos; ninguna llamada al repositorio es real.
- **Frontend:** `vi.restoreAllMocks()` en `beforeEach`; los mocks de `apiClient` son temporales por test.

### 1.3 Cobertura de requisitos funcionales

| Requisito / Regla | Descripción breve | CP cubiertos |
|---|---|---|
| REQ-001 / REQ-002 | Crear solicitud → estado inicial PENDIENTE | CP-001 |
| REQ-003 | Confirmar solicitud sin colisión → CONFIRMADA | CP-002 |
| REQ-004 | Rechazar solicitud PENDIENTE → RECHAZADA | CP-004 |
| REQ-005 | Múltiples solicitudes PENDIENTE para mismo slot permitidas | CP-003 (parte 1) |
| RN-001 | Un slot solo puede tener una reserva CONFIRMADA | CP-003 |
| RN-003 | Transiciones inválidas desde estados terminales rechazadas | CP-005, CP-006, CP-007 |
| RF09 | DOCENTE consulta solo sus solicitudes (`/mis-solicitudes`) | CP-010 |
| RF10 | Consulta de reservas CONFIRMADAS por equipo y fecha | CP-014 |
| CA-001 / CA-002 | Roles DOCENTE y BIBLIOTECARIA con control de acceso | CP-009, CP-011 |
| HU-010 | Auditoría de confirmación y rechazo | CP-015 |
| HU-011 | Consulta de trazabilidad por endpoint | CP-015 |

---

## 2. Entorno de pruebas

| Ítem | Valor |
|---|---|
| Perfil Spring activo | `dev` (H2 in-memory, esquema `schema-h2.sql`, datos `data-h2.sql`) |
| Base de datos tests | H2 (modo embedded, `spring.jpa.hibernate.ddl-auto=none`) |
| Framework test backend | JUnit Jupiter 5.x, Mockito 5.x, Spring Boot Test, Spring Security Test, AssertJ, JSONPath |
| Framework test frontend | Vitest 2.1.9 |
| Java | 21 |
| Node.js | >=18 (LTS) |
| Comando ejecución backend | `./mvnw test` |
| Comando ejecución frontend | `npx vitest run` |

---

## 3. Trazabilidad: Requisitos → Casos de Prueba

| ID Caso | Requisito(s) / Regla(s) | Nivel | Clase/Archivo de test |
|---|---|---|---|
| CP-001 | REQ-001, REQ-002 | Unitario + Integración | `ReservaServiceTest`, `ReservaControllerIntegrationTest` |
| CP-002 | REQ-003 | Unitario + Integración | `ReservaServiceTest`, `ReservaControllerIntegrationTest` |
| CP-003 | REQ-005, RN-001 | Unitario + Integración | `ReservaServiceTest`, `ReservaControllerIntegrationTest`, `ReservaIntegrationTest` |
| CP-004 | REQ-004 | Unitario + Integración | `ReservaServiceTest`, `ReservaControllerIntegrationTest` |
| CP-005 | RN-003 | Unitario | `ReservaServiceTest` |
| CP-006 | RN-003 | Unitario + Integración | `ReservaServiceTest`, `ReservaIntegrationTest` |
| CP-007 | RN-003 | Unitario | `ReservaServiceTest` |
| CP-008 | REQ-001 (validación equipo) | Unitario + Integración | `ReservaServiceTest`, `ReservaControllerIntegrationTest` |
| CP-009 | CA-001, ADR-005 | Integración (MockMvc) | `ReservaControllerIntegrationTest` |
| CP-010 | RF09, ADR-011 | Unitario + Integración + Frontend | `ReservaServiceTest`, `ReservaControllerIntegrationTest`, `rolesAndEndpoints.test.ts` |
| CP-011 | CA-002, ADR-011 | Integración (MockMvc) + Frontend | `ReservaControllerIntegrationTest`, `rolesAndEndpoints.test.ts` |
| CP-012 | REQ-001..005, RN-001, RN-003 | Integración end-to-end | `ReservaIntegrationTest`, `ReservaControllerIntegrationTest` |
| CP-013 | Validación Bean Validation | Integración (MockMvc) | `ReservaControllerIntegrationTest` |
| CP-014 | RF10, ADR-012 | Unitario + Integración + Frontend | `ReservaServiceTest`, `ReservaControllerIntegrationTest`, `rolesAndEndpoints.test.ts` |
| CP-015 | HU-010, HU-011, ADR-010 | Integración | `AuditoriaIntegrationTest` |

---

## 4. Catálogo de casos de prueba

### CP-001: Crear solicitud en estado PENDIENTE

**Requisitos:** REQ-001, REQ-002  
**Nivel:** Unitario + MockMvc  
**Precondición:** Equipo con ID válido existente. Usuario autenticado con rol `ROLE_DOCENTE`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v1/reservas` con `{ docenteId, equipoId, fecha, modulo }` válidos | HTTP 201 Created |
| 2 | Body de respuesta: campo `estado` | `"PENDIENTE"` |
| 3 | Body de respuesta: campo `fechaCreacion` | No nulo |
| 4 | Body de respuesta: campo `equipoNombre` | Nombre del equipo referenciado |
| 5 | El repositorio invoca `save(Reserva)` exactamente una vez | Verificado con Mockito |

**Tests que cubren:**
- `ReservaServiceTest#debeCrearSolicitudEnEstadoPendiente` — unitario, Mockito
- `ReservaControllerIntegrationTest#docenteAutenticadoPuedeCrearSolicitud` — MockMvc + `@WithMockUser`

**Estado:** PASS

---

### CP-002: Confirmar solicitud sin colisión → CONFIRMADA

**Requisitos:** REQ-003  
**Nivel:** Unitario + MockMvc  
**Precondición:** Reserva existente en estado `PENDIENTE`. No existe ninguna `CONFIRMADA` para el mismo slot. Usuario autenticado con rol `ROLE_BIBLIOTECARIA`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v1/reservas/{id}/confirmar` | HTTP 200 OK |
| 2 | Body: campo `estado` | `"CONFIRMADA"` |
| 3 | `existsByEquipoIdAndFechaAndModuloAndEstado` devuelve `false` | Verificado |
| 4 | Se llama a `save(reserva)` con el nuevo estado | Verificado |

**Tests que cubren:**
- `ReservaServiceTest#debeConfirmarSolicitudExitosamenteCuandoNoHayColision` — unitario
- `ReservaControllerIntegrationTest#bibliotecariaPuedeConfirmarSolicitud` — MockMvc

**Estado:** PASS

---

### CP-003: Colisión al confirmar (RN-001) → 409 Conflict

**Requisitos:** REQ-005, RN-001  
**Nivel:** Unitario + MockMvc + Integración  
**Precondición:** Existe una reserva `CONFIRMADA` (Solicitud A) para el slot `(equipoId, fecha, modulo)`. Se intenta confirmar Solicitud B para el mismo slot.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v1/reservas/{idB}/confirmar` | HTTP 409 Conflict |
| 2 | Body: campo `status` | `409` |
| 3 | Body: campo `codigo` | `"RESERVA_COLISION_CONFIRMADA"` |
| 4 | Body: campo `mensaje` | Contiene `"Ya existe otra reserva confirmada"` |
| 5 | Solicitud B permanece en estado `PENDIENTE` | Verificado con `never().save()` |
| 6 | Múltiples solicitudes `PENDIENTE` para el mismo slot sí son permitidas en la creación | Verificado |

**Tests que cubren:**
- `ReservaServiceTest#noDebeConfirmarSolicitudSiYaExisteOtraConfirmadaParaElMismoSlot` — unitario
- `ReservaControllerIntegrationTest#flujoCompletoColisionReglaRN001` — MockMvc
- `ReservaIntegrationTest#testFlujoCompletoConcurrenciaYConflicto` — integración e2e
- `AuditoriaIntegrationTest#cp15_operacionFallidaPorColisionNoGeneraAuditoria` — integración

**Estado:** PASS

---

### CP-004: Rechazar solicitud PENDIENTE → RECHAZADA

**Requisitos:** REQ-004  
**Nivel:** Unitario + MockMvc  
**Precondición:** Reserva en estado `PENDIENTE`. Usuario autenticado con rol `ROLE_BIBLIOTECARIA`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v1/reservas/{id}/rechazar` | HTTP 200 OK |
| 2 | Body: campo `estado` | `"RECHAZADA"` |
| 3 | El repositorio guarda el nuevo estado | Verificado |

**Tests que cubren:**
- `ReservaServiceTest#debeRechazarSolicitudExitosamente` — unitario
- `ReservaControllerIntegrationTest#bibliotecariaPuedeRechazarSolicitud` — MockMvc

**Estado:** PASS

---

### CP-005: Transición inválida CONFIRMADA → CONFIRMADA (RN-003)

**Requisitos:** RN-003 (estados terminales no pueden retransicionarse)  
**Nivel:** Unitario  
**Precondición:** Reserva en estado `CONFIRMADA`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Llamar a `confirmarSolicitud(id)` | Lanza `InvalidStateTransitionException` |
| 2 | Mensaje de excepción | Contiene `"Solo se pueden confirmar solicitudes en estado PENDIENTE"` |
| 3 | No se llama a `save()` | Verificado con `never()` |

**Tests que cubren:**
- `ReservaServiceTest#noDebeConfirmarSolicitudSiYaEstaConfirmada` — unitario

> **Nota:** En la capa HTTP, `InvalidStateTransitionException` se mapea a HTTP 409 con código `TRANSICION_ESTADO_INVALIDA` (verificado en `flujoCompletoColisionReglaRN001`).

**Estado:** PASS

---

### CP-006: Transición inválida CONFIRMADA → RECHAZADA (RN-003)

**Requisitos:** RN-003  
**Nivel:** Unitario  
**Precondición:** Reserva en estado `CONFIRMADA`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Llamar a `rechazarSolicitud(id)` | Lanza `InvalidStateTransitionException` |
| 2 | Mensaje | Contiene `"Solo se pueden rechazar solicitudes en estado PENDIENTE"` |
| 3 | No se llama a `save()` | Verificado |

**Tests que cubren:**
- `ReservaServiceTest#noDebeRechazarSolicitudSiYaEstaConfirmada` — unitario

**Estado:** PASS

---

### CP-007: Transición inválida RECHAZADA → CONFIRMADA (RN-003)

**Requisitos:** RN-003  
**Nivel:** Unitario + Integración  
**Precondición:** Reserva en estado `RECHAZADA`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Llamar a `confirmarSolicitud(id)` | Lanza `InvalidStateTransitionException` |
| 2 | En capa HTTP: `POST /api/v1/reservas/{id}/confirmar` | HTTP 409 con `codigo: TRANSICION_ESTADO_INVALIDA` |
| 3 | No se modifica el estado | Verificado |

**Tests que cubren:**
- `ReservaServiceTest#noDebeConfirmarSolicitudSiYaEstaRechazada` — unitario
- `ReservaIntegrationTest#testFlujoCompletoConcurrenciaYConflicto` (paso final) — integración

**Estado:** PASS

---

### CP-008: Equipo inexistente → 404 Not Found

**Requisitos:** REQ-001 (validación de equipo al crear)  
**Nivel:** Unitario + MockMvc  
**Precondición:** Se envía un `equipoId` que no existe en la base de datos.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v1/reservas` con `equipoId=99999` | HTTP 404 Not Found |
| 2 | Body: campo `codigo` | `"RECURSO_NO_ENCONTRADO"` |
| 3 | Unitario: `crearSolicitud(request)` | Lanza `ResourceNotFoundException` con mensaje que contiene el ID |

**Tests que cubren:**
- `ReservaServiceTest#debeLanzarExcepcionSiEquipoNoExiste` — unitario
- `ReservaServiceTest#debeLanzarExcepcionSiReservaNoExisteAlConfirmar` — unitario (reserva inexistente)
- `ReservaControllerIntegrationTest#debeRetornar404CuandoEquipoNoExiste` — MockMvc

**Estado:** PASS

---

### CP-009: Control de acceso por rol (DOCENTE no puede confirmar/rechazar)

**Requisitos:** CA-001, ADR-005  
**Nivel:** MockMvc (Spring Security Test)  
**Precondición:** Usuario autenticado con rol `ROLE_DOCENTE`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v1/reservas/{id}/confirmar` con rol DOCENTE | HTTP 403 Forbidden |
| 2 | Body: campo `codigo` | `"ACCESO_DENEGADO"` |
| 3 | Body: campo `status` | `403` |
| 4 | `POST /api/v1/reservas/{id}/rechazar` con rol DOCENTE | HTTP 403 Forbidden |
| 5 | Body: campo `codigo` | `"ACCESO_DENEGADO"` |
| 6 | Usuario no autenticado en cualquier endpoint protegido | HTTP 401 con `codigo: NO_AUTENTICADO` |

**Tests que cubren:**
- `ReservaControllerIntegrationTest#docenteNoDebePoderConfirmar` — MockMvc `@WithMockUser(roles="DOCENTE")`
- `ReservaControllerIntegrationTest#docenteNoDebePoderRechazar` — MockMvc
- `ReservaControllerIntegrationTest#usuarioNoAutenticadoNoDebePoderConfirmar` — MockMvc (sin auth)
- `ReservaControllerIntegrationTest#usuarioNoAutenticadoNoDebePoderRechazar` — MockMvc (sin auth)
- `ReservaControllerIntegrationTest#usuarioNoAutenticadoNoDebePoderCrearSolicitud` — MockMvc (sin auth)

**Estado:** PASS

---

### CP-010: DOCENTE consulta sus propias solicitudes (mis-solicitudes)

**Requisitos:** RF09, ADR-011  
**Nivel:** Unitario + MockMvc + Frontend  
**Precondición:** DOCENTE 1 y DOCENTE 2 tienen reservas. Ambos autenticados vía HTTP Basic.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `GET /api/v1/reservas/mis-solicitudes` autenticado como `docente` | HTTP 200; lista contiene solo reservas del `docenteId=1` |
| 2 | `GET /api/v1/reservas/mis-solicitudes` autenticado como `docente2` | HTTP 200; lista contiene solo reservas del `docenteId=2` |
| 3 | `GET /api/v1/reservas` autenticado como `docente` | HTTP 200; filtrado automático a sus solicitudes |
| 4 | Frontend: `reservaApi.listarMisSolicitudes()` | Llama a `GET /reservas/mis-solicitudes`; devuelve solo las del usuario autenticado |
| 5 | Servicio: `listarMisSolicitudes(docenteId)` invoca `findByDocenteId(docenteId)` | Verificado con Mockito |

**Tests que cubren:**
- `ReservaServiceTest#debeListarMisSolicitudesFiltradasPorDocenteId` — unitario
- `ReservaControllerIntegrationTest#cp10_docenteConsultaSusPropiasSolicitudes` — MockMvc
- `ReservaControllerIntegrationTest#cp10_docenteConsultaReservasGeneralesSoloVeLasSuyas` — MockMvc
- `rolesAndEndpoints.test.ts` (describe RF09 & CP10) — Vitest, 2 tests

**Estado:** PASS

---

### CP-011: DOCENTE no puede acceder a solicitudes de otro docente

**Requisitos:** CA-002, ADR-011 (aislamiento de datos por identidad)  
**Nivel:** MockMvc + Frontend

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `GET /api/v1/reservas/{id}` con reserva de DOCENTE 2, autenticado como DOCENTE 1 | HTTP 403 `ACCESO_DENEGADO` |
| 2 | `GET /api/v1/reservas?docenteId=2` autenticado como DOCENTE 1 | HTTP 403 `ACCESO_DENEGADO` |
| 3 | BIBLIOTECARIA puede acceder a cualquier reserva (acceso administrativo) | HTTP 200 OK |
| 4 | `GET /api/v1/reservas/mis-solicitudes` sin autenticación | HTTP 401 `NO_AUTENTICADO` |
| 5 | BIBLIOTECARIA en `/mis-solicitudes` (endpoint exclusivo DOCENTE) | HTTP 403 `ACCESO_DENEGADO` |
| 6 | Frontend: error 403 en `reservaApi.obtenerPorId(999)` | Propaga `ApiError` con `status=403` y `codigo=ACCESO_DENEGADO` |

**Tests que cubren:**
- `ReservaControllerIntegrationTest#cp11_docenteNoPuedeAccederADetalleDeOtroDocente` — MockMvc
- `ReservaControllerIntegrationTest#cp11_docenteNoPuedeFiltrarPorIdDeOtroDocente` — MockMvc
- `ReservaControllerIntegrationTest#cp11_bibliotecariaPuedeConsultarCualquierSolicitud` — MockMvc
- `ReservaControllerIntegrationTest#cp11_usuarioNoAutenticadoEnMisSolicitudesRetorna401` — MockMvc
- `ReservaControllerIntegrationTest#cp11_bibliotecariaEnMisSolicitudesRetorna403` — MockMvc
- `rolesAndEndpoints.test.ts` (describe CP11) — Vitest, 1 test

**Estado:** PASS

---

### CP-012: Flujo completo end-to-end: crear → confirmar → colisión → rechazar

**Requisitos:** REQ-001 al REQ-005, RN-001, RN-003  
**Nivel:** Integración end-to-end (SpringBootTest + MockMvc + HTTP Basic real)  
**Descripción:** Reproduce el flujo completo del caso de uso principal, incluyendo caminos alternativos de error.

| # | Paso | Resultado esperado |
|---|---|---|
| 1 | DOCENTE 1 crea Solicitud A (slot X) | HTTP 201, estado `PENDIENTE` |
| 2 | BIBLIOTECARIA confirma Solicitud A | HTTP 200, estado `CONFIRMADA` |
| 3 | DOCENTE 2 crea Solicitud B (mismo slot X) | HTTP 201, estado `PENDIENTE` (coexistencia permitida) |
| 4 | BIBLIOTECARIA intenta confirmar Solicitud B | HTTP 409, `RESERVA_COLISION_CONFIRMADA` |
| 5 | Verificación: Solicitud B sigue `PENDIENTE` | HTTP 200 al consultar — `PENDIENTE` |
| 6 | BIBLIOTECARIA rechaza Solicitud B | HTTP 200, estado `RECHAZADA` |
| 7 | Intentar confirmar Solicitud B (ya `RECHAZADA`) | HTTP 409, `TRANSICION_ESTADO_INVALIDA` |

**Tests que cubren:**
- `ReservaIntegrationTest#testFlujoCompletoConcurrenciaYConflicto` — integración (pasos 1–7)
- `ReservaControllerIntegrationTest#flujoCompletoColisionReglaRN001` — MockMvc (pasos 1–6 con múltiples asserts)

**Estado:** PASS

---

### CP-013: Validación de campos obligatorios → 400 Bad Request

**Requisitos:** SSD sección 6 (validación de entrada)  
**Nivel:** MockMvc  
**Precondición:** Usuario autenticado con rol `ROLE_DOCENTE`.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `POST /api/v1/reservas` con body `{}` (vacío) | HTTP 400 Bad Request |
| 2 | Body: campo `codigo` | `"DATOS_INVALIDOS"` |
| 3 | Body: campo `status` | `400` |
| 4 | Body: campo `validaciones.docenteId` | Mensaje de error de validación presente |
| 5 | Body: campo `validaciones.equipoId` | Mensaje de error de validación presente |
| 6 | Body: campo `validaciones.fecha` | Mensaje de error de validación presente |
| 7 | Body: campo `validaciones.modulo` | Mensaje de error de validación presente |

**Tests que cubren:**
- `ReservaControllerIntegrationTest#debeRetornar400CuandoFaltanCampos` — MockMvc

**Estado:** PASS

---

### CP-014: Consulta de reservas CONFIRMADAS por equipo y fecha

**Requisitos:** RF10, ADR-012  
**Nivel:** Unitario + MockMvc + Frontend

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | `GET /api/v1/reservas/confirmadas?equipoId=X&fecha=YYYY-MM-DD` | HTTP 200; solo reservas `CONFIRMADA` para ese equipo y fecha |
| 2 | Reservas `PENDIENTE` o `RECHAZADA` para el mismo equipo y fecha | No incluidas en el resultado |
| 3 | Reservas `CONFIRMADA` de otra fecha | No incluidas en el resultado |
| 4 | `GET /api/v1/reservas/confirmadas?fecha=YYYY-MM-DD` (sin equipoId) | HTTP 200; todas las `CONFIRMADA` de esa fecha |
| 5 | Sin autenticación | HTTP 401 `NO_AUTENTICADO` |
| 6 | Servicio: `listarConfirmadas(equipoId, fecha)` invoca `findByEquipoIdAndFechaAndEstado` | Verificado con Mockito |
| 7 | Servicio: `listarConfirmadas(null, fecha)` invoca `findByFechaAndEstado` | Verificado con Mockito |
| 8 | Frontend: `reservaApi.listarConfirmadas({equipoId, fecha})` | Llama a `/reservas/confirmadas?equipoId=X&fecha=Y` |

**Tests que cubren:**
- `ReservaServiceTest#debeListarConfirmadasPorEquipoYFecha` — unitario
- `ReservaServiceTest#debeListarConfirmadasSoloPorFecha` — unitario
- `ReservaRepositoryTest#shouldFilterConfirmedReservasByEquipoAndFecha` — `@DataJpaTest`
- `ReservaControllerIntegrationTest#cp14_consultaReservasConfirmadasPorRecursoYFecha` — MockMvc
- `ReservaControllerIntegrationTest#cp14_consultaReservasConfirmadasSoloPorFecha` — MockMvc
- `ReservaControllerIntegrationTest#cp14_consultaConfirmadasSinAutenticacionRetorna401` — MockMvc
- `rolesAndEndpoints.test.ts` (describe RF10 & CP14) — Vitest, 3 tests

**Estado:** PASS

---

### CP-015: Auditoría de operaciones (registros de trazabilidad)

**Requisitos:** HU-010, HU-011, ADR-010  
**Nivel:** Integración (SpringBootTest + repositorio de auditoría)

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | BIBLIOTECARIA confirma solicitud | 1 registro en `auditoria_reservas`: `operacion=CONFIRMAR`, `resultado=EXITOSO`, `usuario=bibliotecaria`, `fechaHora` dentro del rango esperado |
| 2 | BIBLIOTECARIA rechaza solicitud | 1 registro: `operacion=RECHAZAR`, `resultado=EXITOSO` |
| 3 | Confirmación fallida por colisión (409) | No se crea registro de auditoría para la solicitud que falló |
| 4 | Confirmación fallida por transición inválida (409) | Solo persiste el registro de la operación exitosa anterior; no se crea registro falso |
| 5 | Intento de confirmación bloqueado por 403 (DOCENTE) | No se crea ningún registro de auditoría |
| 6 | `GET /api/v1/reservas/{id}/auditoria` como BIBLIOTECARIA | HTTP 200; lista los registros con `reservaId`, `usuario`, `operacion`, `resultado`, `fechaHora` |
| 7 | `GET /api/v1/reservas/{id}/auditoria` como DOCENTE | HTTP 403 `ACCESO_DENEGADO` |

**Tests que cubren:**
- `AuditoriaIntegrationTest#cp15_confirmacionGeneraAuditoriaCorrecta`
- `AuditoriaIntegrationTest#cp15_rechazoGeneraAuditoriaCorrecta`
- `AuditoriaIntegrationTest#cp15_operacionFallidaPorColisionNoGeneraAuditoria`
- `AuditoriaIntegrationTest#cp15_operacionFallidaPorTransicionInvalidaNoGeneraAuditoria`
- `AuditoriaIntegrationTest#cp15_operacionRechazadaPorAutorizacionNoGeneraAuditoria`
- `AuditoriaIntegrationTest#hu11_consultaDeTrazabilidadPorEndpoint`

**Estado:** PASS

---

## 5. Cobertura de pruebas de seguridad

Los aspectos de seguridad quedan cubiertos transversalmente por los tests de integración con Spring Security Test:

| Escenario de seguridad | Código HTTP esperado | Verificado en |
|---|---|---|
| Usuario no autenticado en endpoint protegido | 401 `NO_AUTENTICADO` | `ReservaControllerIntegrationTest` (3 tests) |
| DOCENTE intenta confirmar (rol insuficiente) | 403 `ACCESO_DENEGADO` | CP-009 |
| DOCENTE intenta rechazar (rol insuficiente) | 403 `ACCESO_DENEGADO` | CP-009 |
| DOCENTE accede a detalle de reserva de otro docente | 403 `ACCESO_DENEGADO` | CP-011 |
| DOCENTE filtra por `docenteId` de otro docente | 403 `ACCESO_DENEGADO` | CP-011 |
| BIBLIOTECARIA accede a `/mis-solicitudes` (rol DOCENTE requerido) | 403 `ACCESO_DENEGADO` | CP-011 |
| DOCENTE accede a endpoint de auditoría (rol BIBLIOTECARIA requerido) | 403 `ACCESO_DENEGADO` | CP-015 |
| `GET /api/v1/equipos` sin autenticación | 200 OK (endpoint público) | `ReservaControllerIntegrationTest#equiposEsEndpointPublico` |
| `GET /api/v1/health` sin autenticación | 200 OK (endpoint público) | `HealthControllerTest#shouldReturnHealthStatusUpAndDbConnected` |

**ADR de referencia:** ADR-005 (modelo de roles), ADR-006 (contrato de errores).

---

## 6. Pruebas de frontend

**Framework:** Vitest 2.1.9 | **Archivos de test:** 3 | **Tests totales:** 15

### 6.1 Inventario de suites

| Archivo | Suite | Tests | Estado |
|---|---|---|---|
| `reservaRules.test.ts` | Reglas de Estados del Frontend (SSD / MVP) | 2 | PASS |
| `apiClient.test.ts` | ApiClient & ApiError | 5 | PASS |
| `rolesAndEndpoints.test.ts` | Roles, Endpoints y Casos de Prueba (CP10, CP11, CP14) | 8 | PASS |

### 6.2 Descripción por suite

**`reservaRules.test.ts`** — Verifica invariantes del modelo de datos:
- Los tres estados válidos `PENDIENTE | CONFIRMADA | RECHAZADA` están presentes.
- El estado inicial de una nueva solicitud es `PENDIENTE` (no "Reserva realizada" ni otro texto).

**`apiClient.test.ts`** — Verifica `ApiError` y gestión de credenciales HTTP Basic:
- Construcción correcta de errores 409 (`RESERVA_COLISION_CONFIRMADA`), 400 (`DATOS_INVALIDOS`), 401 (`NO_AUTENTICADO`), 403 (`ACCESO_DENEGADO`).
- `setAuthCredentials` / `getAuthCredentials` / `clearAuthCredentials` gestionan el header Basic en memoria.

**`rolesAndEndpoints.test.ts`** — Verifica contratos de API desde el cliente:
- CP-010: `reservaApi.listarMisSolicitudes()` llama a `/reservas/mis-solicitudes`; solo retorna reservas del docente autenticado.
- CP-011: `reservaApi.obtenerPorId(id)` propaga `ApiError` con `status=403`.
- CP-014: `reservaApi.listarConfirmadas({equipoId, fecha})` llama a `/reservas/confirmadas?equipoId=X&fecha=Y`; solo retorna `CONFIRMADA`.
- Manejo de error 409 `RESERVA_COLISION_CONFIRMADA` en `reservaApi.confirmar()`.

---

## 7. Brechas de cobertura y pruebas pendientes

Las siguientes áreas no están cubiertas por tests automáticos en esta versión del MVP. Se documentan como Pendientes y no implican defectos en el código actual.

| ID | Area | Descripción de brecha | Prioridad |
|---|---|---|---|
| B-01 | E2E / integración de UI | No existe suite E2E (Cypress / Playwright). Los flujos de usuario completos (login → crear → confirmar) solo se prueban manualmente. | Alta |
| B-02 | Tests de componentes React | No existen tests con React Testing Library (RTL) ni Storybook. La UI de `ReservasPage.tsx`, el formulario de creación y el panel de la bibliotecaria no tienen cobertura automatizada. | Alta |
| B-03 | Concurrencia a nivel de BD | La garantía de unicidad de slot confirmado es defensiva en el `Service`. El índice parcial único de PostgreSQL (`schema-postgres.sql`) no se activa en el perfil `dev` (H2). No existe test de carga o concurrencia. | Media |
| B-04 | Perfil `prod` / PostgreSQL | Toda la suite corre sobre H2. No existen tests de integración contra PostgreSQL real. | Media |
| B-05 | Motivo de rechazo | ADR-009 documenta la exclusión del motivo por ser fuera del MVP. Si se implementa en el futuro, requiere casos CP nuevos. | Baja |
| B-06 | Cancelación de reservas confirmadas | El SSD no incluye cancelación para el MVP (ADR-009). Sin test de intento de cancelación. | Baja |
| B-07 | Accesibilidad (a11y) | No existen tests de accesibilidad (axe-core, Lighthouse). | Baja |
| B-08 | Paginación / límites de resultados | Los endpoints de listado no implementan paginación. No hay test de volumen. | Baja |

---

## 8. Resultados de ejecución

> Los resultados a continuación corresponden a la ejecución real de la suite en la fecha de elaboración de este documento (`2026-09-30`).

### 8.1 Backend (Maven / JUnit)

**Comando:** `./mvnw test`  
**Perfil activo:** `dev` (H2 in-memory)  
**Resultado global:** BUILD SUCCESS — EXIT CODE 0

| Clase de test | Tests | Fallos | Errores | Skipped |
|---|---|---|---|---|
| `BibliotecaHorizonteApplicationTests` | 1 | 0 | 0 | 0 |
| `HealthControllerTest` | 1 | 0 | 0 | 0 |
| `ReservaRepositoryTest` | 2 | 0 | 0 | 0 |
| `ReservaServiceTest` | 11 | 0 | 0 | 0 |
| `ReservaControllerIntegrationTest` | 18 | 0 | 0 | 0 |
| `ReservaIntegrationTest` | 1 | 0 | 0 | 0 |
| `AuditoriaIntegrationTest` | 6 | 0 | 0 | 0 |
| **TOTAL** | **40** | **0** | **0** | **0** |

### 8.2 Frontend (Vitest)

**Comando:** `npx vitest run`  
**Resultado global:** 3 test files passed — EXIT CODE 0

| Archivo | Tests | Estado |
|---|---|---|
| `reservaRules.test.ts` | 2 | PASS |
| `apiClient.test.ts` | 5 | PASS |
| `rolesAndEndpoints.test.ts` | 8 | PASS |
| **TOTAL** | **15** | **PASS** |

---

## 9. Riesgos y notas

| # | Riesgo | Impacto | Mitigación actual |
|---|---|---|---|
| R-01 | Condición de carrera en confirmación concurrente | Alto | Validación lógica en el servicio (ADR-003); índice parcial único en PostgreSQL (`schema-postgres.sql`) no activo en H2. Riesgo residual en producción sin transacción con nivel `SERIALIZABLE`. |
| R-02 | Ausencia de pruebas E2E | Medio | Cobertura de integración MockMvc como sustituto de confianza para el MVP. |
| R-03 | Datos estáticos en `data-h2.sql` | Bajo | Las credenciales de test (docente/docente123, bibliotecaria/biblio123) están hardcodeadas. No usar en producción. |
| R-04 | Divergencia H2 vs. PostgreSQL | Medio | El índice parcial condicional de PostgreSQL no se replica en H2. La regla RN-001 se verifica solo a nivel de aplicación en los tests. |

---

*Documento generado a partir del código fuente real del repositorio. Última ejecución verificada: 2026-09-30.*

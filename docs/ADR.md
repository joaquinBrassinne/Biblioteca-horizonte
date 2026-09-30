# Registro de Decisiones de Arquitectura (ADR)
## Proyecto: Biblioteca Horizonte - MVP Gestión de Reservas

Este documento compila las Decisiones de Arquitectura de Software (Architecture Decision Records) adoptadas en el proyecto **Biblioteca Horizonte**. Cada registro documenta el contexto técnico, la decisión adoptada, las alternativas descartadas, las consecuencias y la evidencia comprobada en el código fuente.

---

## Índice de Registros

1. [ADR-001: Arquitectura desacoplada (React SPA + Spring Boot REST + BD Relacional)](#adr-001-arquitectura-desacoplada-react-spa--spring-boot-rest--bd-relacional)
2. [ADR-002: Máquina de estados de la reserva y transiciones terminales](#adr-002-máquina-de-estados-de-la-reserva-y-transiciones-terminales)
3. [ADR-003: Estrategia de prevención de colisiones (RN-001) y mitigación de condiciones de carrera](#adr-003-estrategia-de-prevención-de-colisiones-rn-001-y-mitigación-de-condiciones-de-carrera)
4. [ADR-004: Manejo de fechas como LocalDate sin componente de huso horario](#adr-004-manejo-de-fechas-como-localdate-sin-componente-de-huso-horario)
5. [ADR-005: Modelo de roles y autorización en el backend (Spring Security)](#adr-005-modelo-de-roles-y-autorización-en-el-backend-spring-security)
6. [ADR-006: Contrato de errores estandarizado y mapeo a códigos HTTP](#adr-006-contrato-de-errores-estandarizado-y-mapeo-a-códigos-http)
7. [ADR-007: Coexistencia de solicitudes PENDIENTE y persistencia sin rechazo automático](#adr-007-coexistencia-de-solicitudes-pendiente-y-persistencia-sin-rechazo-automático)
8. [ADR-008: Catálogos de módulos horarios, equipos y docentes](#adr-008-catálogos-de-módulos-horarios-equipos-y-docentes)
9. [ADR-009: Exclusión de motivo de rechazo y cancelación de reservas confirmadas](#adr-009-exclusión-de-motivo-de-rechazo-y-cancelación-de-reservas-confirmadas)
10. [ADR-010: Registro y trazabilidad de auditoría para operaciones críticas](#adr-010-registro-y-trazabilidad-de-auditoría-para-operaciones-críticas)
11. [ADR-011: Aislamiento de consultas por identidad de docente ("Mis Solicitudes")](#adr-011-aislamiento-de-consultas-por-identidad-de-docente-mis-solicitudes)
12. [ADR-012: Endpoint de consulta de asignaciones efectivas confirmadas](#adr-012-endpoint-de-consulta-de-asignaciones-efectivas-confirmadas)

---

## ADR-001: Arquitectura desacoplada (React SPA + Spring Boot REST + BD Relacional)

* **Título:** Arquitectura desacoplada cliente-servidor basada en React SPA, Spring Boot REST y Base de Datos Relacional.
* **Estado:** Aceptada.
* **Contexto:** El sistema de Biblioteca Horizonte requiere gestionar solicitudes de reserva de equipamiento para docentes, garantizando la integridad de los datos, el aislamiento de la lógica de negocio y una experiencia de usuario interactiva y moderna.
* **Decisión:** Se implementa una arquitectura desacoplada en dos niveles de aplicación más almacenamiento relacional:
  1. **Frontend:** Single Page Application (SPA) desarrollada con React 18, TypeScript y Vite. La presentación y gestión del estado residen en el cliente, consumiendo la API mediante `fetch` encapsulado en el cliente HTTP [`apiClient.ts`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/api/apiClient.ts).
  2. **Backend:** API REST construida con Java 17 y Spring Boot 3.3.x, organizada bajo una arquitectura en capas clásica: controladores ([`ReservaController.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java)), servicios ([`ReservaServiceImpl.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java)), repositorios Spring Data JPA ([`ReservaRepository.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/ReservaRepository.java)) y capa de seguridad Spring Security ([`SecurityConfig.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/config/SecurityConfig.java)).
  3. **Persistencia:** Base de datos relacional para garantizar consistencia transaccional ACID: motor H2 en memoria para perfil de desarrollo (`dev`) y PostgreSQL para perfil contenedorizado (`prod`).
* **Alternativas consideradas:**
  * *Monolito server-side rendering (Spring Boot + Thymeleaf):* Descartada por menor fluidez en la interfaz de usuario y acoplamiento de la vista al ciclo de vida del servidor.
  * *Base de datos NoSQL (MongoDB):* Descartada por requerir garantías ACID estrictas, transacciones relacionales e integridad referencial entre equipos y reservas.
* **Consecuencias:**
  * *Positivas:* Desacoplamiento total; la lógica de negocio reside exclusivamente en el servidor; facilidad de desarrollo y testing independiente por capas; capacidad de sustitución de clientes sin afectar el backend.
  * *Negativas:* Necesidad de configurar y mantener políticas de Cross-Origin Resource Sharing (CORS), serialización y tipado duplicado en DTOs Java e interfaces TypeScript.
* **Requisitos o reglas relacionadas:** REQ-001, REQ-006, REQ-007, SSD sección 1, SSD sección 10.
* **Evidencia en el código:**
  * Backend Application: [`BibliotecaHorizonteApplication.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/BibliotecaHorizonteApplication.java).
  * Backend Controller: [`ReservaController.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L24-L26).
  * Frontend Entrypoint: [`main.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/main.tsx) y [`App.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/App.tsx).
  * Frontend Client: [`apiClient.ts`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/api/apiClient.ts#L54-L125).
  * Configuración de ambientes: [`application-dev.properties`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/application-dev.properties) y [`application-prod.properties`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/application-prod.properties).

---

## ADR-002: Máquina de estados de la reserva y transiciones terminales

* **Título:** Máquina de estados determinista de la reserva (`PENDIENTE` → `CONFIRMADA` / `RECHAZADA`) con estados terminales inmutables.
* **Estado:** Aceptada.
* **Contexto:** Las reservas atraviesan un ciclo de vida donde no debe existir ambigüedad sobre su estado de asignación. Se debe asegurar que una solicitud nunca ingrese directamente asignada y que una decisión adoptada no sea alterada de forma descontrolada.
* **Decisión:** Se implementa una máquina de estados modelada mediante la enumeración [`EstadoReserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/EstadoReserva.java):
  1. **Estado Inicial Obligatorio:** Toda reserva creada mediante `POST /api/v1/reservas` se inicializa forzosamente en `PENDIENTE` en [`ReservaServiceImpl.crearSolicitud`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L68), ignorando cualquier estado que intente enviar el cliente.
  2. **Transiciones Permitidas:** Únicamente desde `PENDIENTE` hacia `CONFIRMADA` (mediante `confirmarSolicitud`) o hacia `RECHAZADA` (mediante `rechazarSolicitud`).
  3. **Estados Terminales:** `CONFIRMADA` y `RECHAZADA` son terminales. Todo intento de transición sobre una reserva cuyo estado no sea `PENDIENTE` arroja [`InvalidStateTransitionException`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/InvalidStateTransitionException.java), mapeada a `HTTP 409 Conflict` con código de negocio `TRANSICION_ESTADO_INVALIDA`.
* **Alternativas consideradas:**
  * *Permitir reactivación de reservas rechazadas o cancelación de reservas confirmadas:* Descartada para el MVP por no estar solicitada en el alcance funcional y complejizar la consistencia de inventario.
  * *Permitir creación directa en estado CONFIRMADA por parte de administradores:* Descartada para asegurar que toda asignación pase por el flujo formal de solicitud y validación de colisiones.
* **Consecuencias:**
  * *Positivas:* Invariantes simples y estrictas; prevención de inconsistencias en el ciclo de vida; modelo de datos predecible.
  * *Negativas:* Si una solicitud fue confirmada o rechazada por error, no existe mecanismo en el MVP para revertirla a pendiente sin intervención directa en la base de datos.
* **Requisitos o reglas relacionadas:** REQ-002, REQ-003, REQ-004, RN-002, RN-003, CU-001, CU-003, CU-004, CA-001, CA-003, CA-004, SSD secciones 6 y 7.
* **Evidencia en el código:**
  * Enumeración: [`EstadoReserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/EstadoReserva.java#L3-L7).
  * Inicialización forzada: [`ReservaServiceImpl.java:L68`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L68) (`reserva.setEstado(EstadoReserva.PENDIENTE);`).
  * Validación en confirmación: [`ReservaServiceImpl.java:L157-L162`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L157-L162).
  * Validación en rechazo: [`ReservaServiceImpl.java:L195-L200`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L195-L200).
  * Excepción de transición: [`InvalidStateTransitionException.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/InvalidStateTransitionException.java).

---

## ADR-003: Estrategia de prevención de colisiones (RN-001) y mitigación de condiciones de carrera

* **Título:** Estrategia para garantizar RN-001 y evitar colisiones concurrentes en la confirmación de solicitudes.
* **Estado:** Aceptada con implementación parcial en capa de datos según motor RDBMS / Pendiente de bloqueo pesimista en JPA.
* **Contexto:** La regla **RN-001** prohíbe terminantemente la existencia de más de una reserva en estado `CONFIRMADA` para la misma tupla (`equipo_id`, `fecha`, `modulo`). Dado que múltiples solicitudes pueden coexistir en estado `PENDIENTE` para el mismo slot (**RN-004**), una restricción `UNIQUE` convencional sobre las tres columnas no es viable porque impediría tener más de una solicitud pendiente. Ante dos peticiones concurrentes de confirmación para un mismo slot, existe riesgo de *race condition* si la validación se realiza únicamente a nivel de aplicación en memoria.
* **Decisión:** Se implementa una estrategia defensiva:
  1. **Barrera en Capa de Servicio (Implementada):**
     En [`ReservaServiceImpl.confirmarSolicitud`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L164-L177), bajo una transacción `@Transactional`, se evalúa:
     ```java
     boolean ocupado = reservaRepository.existsByEquipoIdAndFechaAndModuloAndEstado(
         reserva.getEquipo().getId(), reserva.getFecha(), reserva.getModulo(), EstadoReserva.CONFIRMADA
     );
     if (ocupado) {
         throw new ReservaConflictException(...);
     }
     ```
  2. **Barrera en Base de Datos PostgreSQL (Implementada en Script DDL):**
     En [`schema-postgres.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema-postgres.sql#L24-L27) se define un índice único parcial condicional:
     ```sql
     CREATE UNIQUE INDEX IF NOT EXISTS uq_reserva_confirmada_slot 
     ON reservas (equipo_id, fecha, modulo) 
     WHERE estado = 'CONFIRMADA';
     ```
     En [`GlobalExceptionHandler.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/GlobalExceptionHandler.java#L93-L109) se intercepta `DataIntegrityViolationException` y se mapea a `HTTP 409 Conflict` con código `RESERVA_COLISION_CONFIRMADA`.
  3. **Situación en Motor H2 / JPA (Estado Real Comprobado):**
     * En la entidad JPA [`Reserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Reserva.java#L8-L10) solo existe un `@Index(name = "idx_reservas_slot", columnList = "equipo_id, fecha, modulo, estado")` no único.
     * En [`schema.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema.sql#L24) el índice tampoco es único.
     * En [`ReservaRepository.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/ReservaRepository.java) **NO** se implementó bloqueo pesimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
     * Por lo tanto, en desarrollo con H2 (`spring.jpa.hibernate.ddl-auto=update`), la protección frente a concurrencia depende exclusivamente de la comprobación previa en Java.
* **Alternativas consideradas:**
  * *UniqueConstraint JPA en toda la tabla (`@UniqueConstraint(columnNames={"equipo_id", "fecha", "modulo"}):* Descartada porque violaría la regla RN-004 al rechazar múltiples solicitudes pendientes.
  * *Bloqueo pesimista (`SELECT ... FOR UPDATE` o `@Lock(PESSIMISTIC_WRITE)`):* Contemplada en arquitectura pero no implementada aún en el repositorio Java.
* **Consecuencias:**
  * *Positivas:* En entornos PostgreSQL con el script `schema-postgres.sql`, la unicidad física está blindada por el motor de base de datos.
  * *Negativas / Limitaciones:* En el perfil H2 por defecto, un escenario de alta concurrencia simultánea en el mismo milisegundo podría superar la comprobación `existsBy...` antes del `save(...)`.
* **Requisitos o reglas relacionadas:** REQ-005, RN-001, RN-004, CA-005, SSD sección 17 (Riesgo técnico 1 y 2).
* **Evidencia en el código:**
  * Validación Java: [`ReservaServiceImpl.java:L164-L177`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L164-L177).
  * Consulta Repository: [`ReservaRepository.java:L14-L19`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/ReservaRepository.java#L14-L19).
  * Script PostgreSQL con índice condicional: [`schema-postgres.sql:L24-L27`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema-postgres.sql#L24-L27).
  * Excepción y Handlers: [`ReservaConflictException.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/ReservaConflictException.java) y [`GlobalExceptionHandler.java:L61-L75, L93-L109`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/GlobalExceptionHandler.java#L61-L75).

---

## ADR-004: Manejo de fechas como LocalDate sin componente de huso horario

* **Título:** Representación y procesamiento de fechas de reserva como `LocalDate` puro (formato ISO-8601 `YYYY-MM-DD`).
* **Estado:** Aceptada.
* **Contexto:** En el contexto escolar, una reserva ocurre en una fecha calendario fija. Si se emplean estructuras de marca temporal completa con huso horario (`ZonedDateTime`, `Instant` o `Date`), las discrepancias de zona horaria entre el cliente (navegador del usuario) y el servidor pueden generar corrimientos involuntarios de día.
* **Decisión:** Se estandariza el manejo de fechas como fecha de calendario pura:
  1. **Backend:** Tipo Java `java.time.LocalDate` en entidades ([`Reserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Reserva.java#L25)), DTOs de petición ([`CrearReservaRequest.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/dto/request/CrearReservaRequest.java#L16)) y DTOs de respuesta ([`ReservaResponse.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/dto/response/ReservaResponse.java#L13)).
  2. **Persistencia:** Columna relacional de tipo `DATE` en las tablas SQL ([`schema-postgres.sql:L16`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema-postgres.sql#L16) y [`schema.sql:L16`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema.sql#L16)).
  3. **Controladores:** Parámetros de consulta tipados con `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` en [`ReservaController.listarConfirmadas`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L94).
  4. **Frontend:** Inputs HTML5 `<input type="date" />` y cadenas en formato estricto `YYYY-MM-DD` en [`reserva.ts`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/types/reserva.ts#L18).
* **Alternativas consideradas:**
  * *Timestamp UNIX epoch (milisegundos) o ISO DateTime (`2026-10-15T00:00:00Z`):* Descartadas por alta probabilidad de corrimiento horario ante clientes en zonas horarias distintas al servidor.
  * *Almacenar como String en base de datos:* Descartada para preservar validaciones de tipo en la base de datos y optimizar índices de búsqueda.
* **Consecuencias:**
  * *Positivas:* Inmunidad a variaciones por horario de verano (DST) o configuración del cliente; serialización predecible y clara.
  * *Negativas:* Ninguna observada en el dominio.
* **Requisitos o reglas relacionadas:** RN-005, REQ-001, CU-001, SSD sección 8, SSD sección 17 (Riesgo técnico 3).
* **Evidencia en el código:**
  * Entidad: [`Reserva.java:L24-L25`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Reserva.java#L24-L25).
  * Request DTO: [`CrearReservaRequest.java:L15-L16`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/dto/request/CrearReservaRequest.java#L15-L16).
  * Endpoint de confirmadas: [`ReservaController.java:L94`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L94).
  * Formulario Frontend: [`ReservaForm.tsx:L147-L156`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/reserva/ReservaForm.tsx#L147-L156).

---

## ADR-005: Modelo de roles y autorización en el backend (Spring Security)

* **Título:** Control de acceso basado en roles (`ROLE_DOCENTE` / `ROLE_BIBLIOTECARIA`) aplicado estrictamente en el backend.
* **Estado:** Aceptada.
* **Contexto:** La decisión abierta nº 1 del SSD cuestionaba si cualquier docente podía confirmar o rechazar solicitudes de otros, o si debía existir un rol bibliotecario diferenciado. Además, la seguridad no podía depender del frontend (ocultar botones), sino que debía garantizarse en el servidor ante peticiones HTTP directas.
* **Decisión:** Se implementa un modelo de autorización RBAC mediante Spring Security y HTTP Basic stateless:
  1. **Roles Definidos:**
     * `ROLE_DOCENTE`: Puede crear solicitudes (`POST /api/v1/reservas`) y consultar únicamente sus propias solicitudes (`GET /api/v1/reservas/mis-solicitudes` o `GET /api/v1/reservas/{id}`). **No** tiene permiso para confirmar, rechazar ni consultar auditoría.
     * `ROLE_BIBLIOTECARIA`: Rol administrativo asignado a Lucía. Autorizado para confirmar (`POST /api/v1/reservas/{id}/confirmar`), rechazar (`POST /api/v1/reservas/{id}/rechazar`), consultar todas las solicitudes (`GET /api/v1/reservas`) y acceder al log de auditoría (`GET /api/v1/reservas/{id}/auditoria`).
  2. **Aplicación en el Backend:**
     * En [`SecurityConfig.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/config/SecurityConfig.java#L74-L85) se configuran las reglas de endpoints:
       * `/confirmar` y `/rechazar` exigen `.hasRole("BIBLIOTECARIA")`.
       * `/mis-solicitudes` exige `.hasRole("DOCENTE")`.
     * Se implementan manejadores REST para respuestas JSON uniformes:
       * [`RestAuthenticationEntryPoint.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/RestAuthenticationEntryPoint.java): Retorna `HTTP 401 Unauthorized` (`NO_AUTENTICADO`).
       * [`RestAccessDeniedHandler.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/RestAccessDeniedHandler.java): Retorna `HTTP 403 Forbidden` (`ACCESO_DENEGADO`) cuando un docente intenta confirmar, rechazar o consultar recursos ajenos.
  3. **Identidad Docente:**
     [`DocenteResolver.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/DocenteResolver.java) mapea el usuario autenticado (ej. `docente1`, `docente2`) con su identificador interno (`docenteId`), sobreescribiendo en [`ReservaController.java:L55-L58`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L55-L58) cualquier valor enviado en el cuerpo de la petición.
* **Alternativas consideradas:**
  * *Seguridad cosmética en React (ocultar botones):* Descartada por constituir una falla de seguridad inaceptable.
  * *Autenticación basada en JWT con base de datos de usuarios:* Descartada para el MVP por sobrecarga operativa frente al alcance establecido.
* **Consecuencias:**
  * *Positivas:* El servidor rechaza con 401 y 403 peticiones ilegítimas; aislamiento real de funciones; resolución formal de la decisión abierta nº 1 del SSD.
  * *Negativas:* Usuarios en memoria (`InMemoryUserDetailsManager`) válidos para desarrollo y testing que deberán migrarse a base de datos en fases futuras.
* **Requisitos o reglas relacionadas:** Decisión abierta nº 1 del SSD, REQ-001, REQ-003, REQ-004, RNF02, RF01, RF05, RF06, RF08, RF12.
* **Evidencia en el código:**
  * Filtro de seguridad: [`SecurityConfig.java:L54-L93, L99-L126`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/config/SecurityConfig.java#L54-L93).
  * Entry Point 401: [`RestAuthenticationEntryPoint.java:L22-L51`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/RestAuthenticationEntryPoint.java#L22-L51).
  * Handler 403: [`RestAccessDeniedHandler.java:L22-L51`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/RestAccessDeniedHandler.java#L22-L51).
  * Resolver de identidad: [`DocenteResolver.java:L20-L45`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/DocenteResolver.java#L20-L45).
  * Frontend Context: [`AuthContext.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/context/AuthContext.tsx) y [`LoginForm.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/auth/LoginForm.tsx).

---

## ADR-006: Contrato de errores estandarizado y mapeo a códigos HTTP

* **Título:** Contrato uniforme de respuestas de error (`ErrorResponse`) y mapeo homogéneo de códigos de estado HTTP (400, 401, 403, 404, 409, 500).
* **Estado:** Aceptada.
* **Contexto:** Las respuestas ante fallos de validación sintáctica, restricciones de dominio, fallas de autenticación/autorización o errores no controlados deben compartir una estructura JSON homogénea y predecible para simplificar el consumo por parte de la SPA.
* **Decisión:** Se implementa el DTO [`ErrorResponse.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/dto/response/ErrorResponse.java) gestionado centralizadamente mediante [`GlobalExceptionHandler.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/GlobalExceptionHandler.java):
  * `400 Bad Request` (`DATOS_INVALIDOS`): Para errores de Bean Validation (`MethodArgumentNotValidException`), incluyendo un mapa de `validaciones` campo -> mensaje.
  * `401 Unauthorized` (`NO_AUTENTICADO`): Emitido cuando no se envían credenciales o son inválidas.
  * `403 Forbidden` (`ACCESO_DENEGADO`): Emitido cuando el usuario autenticado carece de rol suficiente o intenta consultar solicitudes ajenas.
  * `404 Not Found` (`RECURSO_NO_ENCONTRADO`): Emitido ante entidades inexistentes ([`ResourceNotFoundException`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/ResourceNotFoundException.java)).
  * `409 Conflict`:
    * `RESERVA_COLISION_CONFIRMADA`: Si se intenta confirmar un slot ocupado ([`ReservaConflictException`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/ReservaConflictException.java) o `DataIntegrityViolationException`).
    * `TRANSICION_ESTADO_INVALIDA`: Si se intenta transicionar una reserva que no está en `PENDIENTE` ([`InvalidStateTransitionException`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/InvalidStateTransitionException.java)).
  * `500 Internal Server Error` (`ERROR_INTERNO_SERVIDOR`): Fallos imprevistos, enmascarando stack traces sensibles.
* **Alternativas consideradas:**
  * *RFC 7807 Problem Details for HTTP APIs:* Descartada para mantener el payload estricto pactado en la sección 14 del SSD.
  * *Manejo disperso de excepciones con respuestas `@ResponseStatus`:* Descartada por dificultar el mantenimiento y generar inconsistencias en los campos del JSON.
* **Consecuencias:**
  * *Positivas:* Formato homogéneo; fácil consumo en TypeScript mediante la clase [`ApiError`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/api/apiClient.ts#L40-L52); no se filtran datos internos del servidor.
  * *Negativas:* Requiere mantener sincronizados los nombres de códigos de error entre backend y frontend.
* **Requisitos o reglas relacionadas:** SSD sección 14, RN-001, RN-003, CA-005.
* **Evidencia en el código:**
  * DTO: [`ErrorResponse.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/dto/response/ErrorResponse.java).
  * Handler: [`GlobalExceptionHandler.java:L23-L146`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/exception/GlobalExceptionHandler.java#L23-L146).
  * Frontend Adapter: [`apiClient.ts:L40-L52, L73-96`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/api/apiClient.ts#L40-L52).

---

## ADR-007: Coexistencia de solicitudes PENDIENTE y persistencia sin rechazo automático

* **Título:** Coexistencia de múltiples solicitudes en estado `PENDIENTE` para el mismo slot y no cancelación automática en cascada.
* **Estado:** Aceptada.
* **Contexto:** El SSD establece la regla **RN-004** (múltiples solicitudes pueden coexistir en estado `PENDIENTE` para el mismo equipo, fecha y módulo). La decisión abierta nº 4 del SSD planteaba la interrogante: al confirmar una solicitud, ¿las restantes pendientes competidoras deben ser rechazadas automáticamente por el sistema o deben permanecer en `PENDIENTE`?
* **Decisión:** Se adoptó y codificó el siguiente comportamiento:
  1. La creación de solicitudes nunca evalúa colisiones con otras solicitudes `PENDIENTE`.
  2. Al confirmarse una solicitud en [`ReservaServiceImpl.confirmarSolicitud`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L153-L187), únicamente la solicitud seleccionada transiciona a `CONFIRMADA`.
  3. **Las demás solicitudes pendientes competidoras NO se cancelan ni se rechazan automáticamente:** permanecen intactas en estado `PENDIENTE`.
  4. Si con posterioridad la bibliotecaria o el docente intentan confirmar alguna de esas solicitudes pendientes remanentes, el backend evalúa **RN-001** en tiempo de confirmación y rechaza la operación con `HTTP 409 Conflict` (`RESERVA_COLISION_CONFIRMADA`).
* **Alternativas consideradas:**
  * *Rechazo en cascada automático (auto-cancelación de competidoras):* Descartada en la implementación actual para evitar efectos secundarios automáticos no transparentes y mantener la trazabilidad de qué docente solicitó qué recurso.
* **Consecuencias:**
  * *Positivas:* Transacción simple y atómica; preserva la visibilidad y el historial de peticiones de los docentes; no genera cambios de estado silenciosos.
  * *Negativas:* La lista de solicitudes pendientes de la bibliotecaria puede contener registros que ya no podrán ser confirmados hasta que se ejecute el rechazo manual.
* **Requisitos o reglas relacionadas:** RN-001, RN-004, Decisión abierta nº 4 del SSD, CU-001, CU-003.
* **Evidencia en el código:**
  * [`ReservaServiceImpl.java:L153-L187`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L153-L187): No ejecuta ninguna mutación sobre otras entidades `Reserva`.

---

## ADR-008: Catálogos de módulos horarios, equipos y docentes

* **Título:** Catálogo de módulos horarios predefinido en frontend / abierto en backend, y precarga seed para equipos y docentes.
* **Estado:** Aceptada.
* **Contexto:** Las decisiones abiertas nº 2 y nº 3 del SSD requerían definir: (2) la estructura de los módulos horarios (si eran catálogo cerrado institucional o texto libre); y (3) la gestión de docentes y equipos (si se precargaban vía seed o se permitía carga abierta).
* **Decisión:**
  1. **Módulos Horarios:**
     * En backend y persistencia: Se almacena como `String` ([`Reserva.modulo`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Reserva.java#L28), `VARCHAR(50)`), validado con `@NotBlank` en el DTO de entrada.
     * En frontend: Se definió un catálogo institucional predeterminado de 5 módulos fijos en [`ReservaForm.tsx:L12-L18`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/reserva/ReservaForm.tsx#L12-L18):
       * `M1`: Módulo 1 (08:00 - 09:30)
       * `M2`: Módulo 2 (09:40 - 11:10)
       * `M3`: Módulo 3 (11:20 - 12:50)
       * `M4`: Módulo 4 (14:00 - 15:30)
       * `M5`: Módulo 5 (15:40 - 17:10)
  2. **Equipos:**
     * Se modelan como entidad relacional [`Equipo.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Equipo.java).
     * Se precargan mediante scripts seed ([`data-h2.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/data-h2.sql) y [`data-postgres.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/data-postgres.sql)).
     * Se exponen para el formulario de reserva vía [`EquipoController.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/EquipoController.java) (`GET /api/v1/equipos`).
  3. **Docentes:**
     * Se modelan como entidad relacional [`Docente.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Docente.java) precargados mediante seed.
     * En la reserva se vinculan mediante el campo `docente_id` numérico, el cual se asocia con el usuario autenticado a través de [`DocenteResolver.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/DocenteResolver.java).
* **Alternativas consideradas:**
  * *Enum rígido de módulos en Java:* Descartada en el backend para admitir compatibilidad con variaciones en scripts y tests sin forzar migraciones de esquema.
  * *Carga libre de nombres de equipos en texto plano:* Descartada para asegurar integridad referencial y evitar duplicaciones tipográficas.
* **Consecuencias:**
  * *Positivas:* Selección guiada y coherente en la interfaz; integridad referencial en base de datos.
  * *Negativas:* Discrepancia entre la cantidad de equipos en los scripts seed actuales (3 equipos cargados) frente a los 6 recursos mencionados en el informe de auditoría.
* **Requisitos o reglas relacionadas:** Decisiones abiertas nº 2 y nº 3 del SSD, REQ-001, RN-005, RF02.
* **Evidencia en el código:**
  * Frontend módulos: [`ReservaForm.tsx:L12-L18`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/reserva/ReservaForm.tsx#L12-L18).
  * Backend entidad y request: [`Reserva.java:L27-L28`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Reserva.java#L27-L28) y [`CrearReservaRequest.java:L18-L19`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/dto/request/CrearReservaRequest.java#L18-L19).
  * Seed de datos: [`data-h2.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/data-h2.sql) y [`data-postgres.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/data-postgres.sql).
  * Endpoint Equipos: [`EquipoController.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/EquipoController.java).

---

## ADR-009: Exclusión de motivo de rechazo y cancelación de reservas confirmadas

* **Título:** Exclusión del registro de motivo de rechazo y exclusión de cancelación de reservas confirmadas en el MVP.
* **Estado:** Aceptada (Fuera de alcance del MVP).
* **Contexto:** La decisión abierta nº 5 del SSD consultaba si se requería registrar un motivo o fundamentación al pasar una reserva a `RECHAZADA`, y si una reserva `CONFIRMADA` podría cancelarse en el futuro.
* **Decisión:**
  1. **Motivo de Rechazo:** No se implementa en el MVP. La operación `POST /api/v1/reservas/{id}/rechazar` y el método [`ReservaServiceImpl.rechazarSolicitud`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L190-L210) no aceptan parámetros de motivo ni body. La entidad `Reserva` no cuenta con columna para dicho fin.
  2. **Cancelación de Confirmadas:** No se implementa en el MVP. Se mantiene el principio de la regla **RN-003** donde `CONFIRMADA` y `RECHAZADA` son estados terminales. No existe endpoint ni lógica para cancelar una reserva confirmada.
* **Alternativas consideradas:**
  * *Añadir campo opcional `motivo` en DTO de rechazo:* Descartada para evitar complejizar el contrato REST básico acordado.
  * *Habilitar transición `CONFIRMADA` → `CANCELADA`:* Descartada por estar expresamente listada en la sección 3 del SSD ("Fuera de alcance").
* **Consecuencias:**
  * *Positivas:* Se respeta el alcance estricto del MVP; máquina de estados cerrada y simple.
  * *Negativas:* Los docentes no reciben retroalimentación explicativa de por qué se denegó su petición; una asignación confirmada por error no puede ser liberada a través del sistema en esta versión.
* **Requisitos o reglas relacionadas:** Decisión abierta nº 5 del SSD, SSD sección 3, RN-003, REQ-004.
* **Evidencia en el código:**
  * Firma de rechazo: [`ReservaServiceImpl.java:L190-L210`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L190-L210).
  * Endpoint rechazo: [`ReservaController.java:L109-L112`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L109-L112).
  * Ausencia de campos de cancelación/motivo en [`Reserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/Reserva.java).

---

## ADR-010: Registro y trazabilidad de auditoría para operaciones críticas

* **Título:** Trazabilidad de operaciones de confirmación y rechazo mediante entidad y repositorio de auditoría.
* **Estado:** Aceptada.
* **Contexto:** Como resultado de la auditoría técnica previa (RF11 y RNF06), se detectó que el sistema carecía de registro de quién realizaba las acciones críticas sobre las solicitudes, impidiendo la rendición de cuentas del personal bibliotecario.
* **Decisión:** Se implementa un subsistema de auditoría desacoplado:
  1. **Entidad [`AuditoriaReserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/AuditoriaReserva.java):** Mapea la tabla `auditoria_reservas` con atributos `reserva_id`, `usuario`, `operacion` (`"CONFIRMAR"` o `"RECHAZAR"`), `fecha_hora` y `resultado` (`"EXITOSO"`).
  2. **Servicio [`AuditoriaServiceImpl.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/AuditoriaServiceImpl.java):** Inyecta el usuario autenticado a través de `SecurityContextHolder.getContext().getAuthentication().getName()`.
  3. **Disparo:** Se invoca sincrónicamente dentro de la transacción de confirmación y rechazo en [`ReservaServiceImpl.java:L182-L184, L205-L207`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L182-L184).
  4. **Consulta:** Se expone el endpoint `GET /api/v1/reservas/{id}/auditoria` en [`ReservaController.java:L114-L121`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L114-L121), protegido con `@PreAuthorize("hasRole('BIBLIOTECARIA')")`.
* **Alternativas consideradas:**
  * *Spring Data Envers:* Descartada por sobrecarga de tablas y configuración frente a un requerimiento puntual de dos eventos.
  * *Columnas de auditoría (`usuario_modificacion`) dentro de la entidad `Reserva`:* Descartada porque sobreescribiría el historial impidiendo auditar múltiples transiciones secuenciales.
* **Consecuencias:**
  * *Positivas:* Registro inmutable de transiciones críticas; cumplimiento de los requisitos RF11 y RNF06; aislamiento por rol para la consulta de auditoría.
  * *Negativas:* Inserción secundaria en base de datos en cada confirmación y rechazo.
* **Requisitos o reglas relacionadas:** RF11, RNF06.
* **Evidencia en el código:**
  * Entidad: [`AuditoriaReserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/AuditoriaReserva.java).
  * Servicio: [`AuditoriaServiceImpl.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/AuditoriaServiceImpl.java).
  * Repositorio: [`AuditoriaReservaRepository.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/AuditoriaReservaRepository.java).
  * Invocación: [`ReservaServiceImpl.java:L182-L184, L205-L207`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L182-L184).
  * Endpoint: [`ReservaController.java:L114-L121`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L114-L121).

---

## ADR-011: Aislamiento de consultas por identidad de docente ("Mis Solicitudes")

* **Título:** Aislamiento de consultas por identidad docente (`/api/v1/reservas/mis-solicitudes`) y protección de acceso a solicitudes ajenas (HTTP 403).
* **Estado:** Aceptada.
* **Contexto:** En el análisis del MVP original se detectó que el endpoint `GET /api/v1/reservas` listaba todas las reservas indistintamente (RF09) y un docente podía consultar detalles de solicitudes de otros docentes (CP11), violando la privacidad y el aislamiento de identidad.
* **Decisión:** Se implementó una política de aislamiento garantizada por el servidor:
  1. Se creó el endpoint `GET /api/v1/reservas/mis-solicitudes` en [`ReservaController.java:L84-L89`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L84-L89) anotado con `@PreAuthorize("hasRole('DOCENTE')")`, que obtiene el ID del docente autenticado vía [`DocenteResolver`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/security/DocenteResolver.java) y filtra por [`reservaRepository.findByDocenteId(docenteId)`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/ReservaRepository.java#L21).
  2. En el endpoint general `GET /api/v1/reservas`, si el usuario autenticado tiene rol `ROLE_DOCENTE` y no es bibliotecaria, se restringe la respuesta a sus solicitudes propias. Si se envía un `docenteId` ajeno en los parámetros, se arroja `AccessDeniedException` (HTTP 403 Forbidden).
  3. En `GET /api/v1/reservas/{id}`, el método `verificarAccesoDocente` valida que si el solicitante es docente, el `docenteId` de la reserva coincida con el usuario autenticado; en caso contrario, arroja `AccessDeniedException` (HTTP 403 Forbidden).
  4. En el frontend, el componente [`ReservasPage.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/pages/ReservasPage.tsx#L57-L58) consume `reservaApi.listarMisSolicitudes()` y el componente [`VerificarAccesoSolicitud.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/reserva/VerificarAccesoSolicitud.tsx) comprueba el aislamiento ante solicitudes ajenas.
* **Alternativas consideradas:**
  * *Filtrar únicamente en React:* Descartada de forma absoluta por permitir fuga de información mediante inspección de tráfico de red.
* **Consecuencias:**
  * *Positivas:* Privacidad garantizada; cumplimiento de RF09, CP10 y CP11.
  * *Negativas:* Requiere que la sesión del cliente mantenga sincronizadas las credenciales HTTP Basic con el legajo del docente.
* **Requisitos o reglas relacionadas:** RF09, CP10, CP11, CU-002.
* **Evidencia en el código:**
  * Verificación en Servicio: [`ReservaServiceImpl.java:L134-L149`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L134-L149).
  * Controladores: [`ReservaController.java:L63-L89, L99-L102`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L63-L89).
  * Frontend: [`reservaApi.ts:L21-L26`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/api/reservaApi.ts#L21-L26) y [`VerificarAccesoSolicitud.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/reserva/VerificarAccesoSolicitud.tsx).

---

## ADR-012: Endpoint de consulta de asignaciones efectivas confirmadas

* **Título:** Endpoint especializado para consulta de asignaciones confirmadas (`/api/v1/reservas/confirmadas`) con filtros por recurso y fecha.
* **Estado:** Aceptada.
* **Contexto:** El personal bibliotecario y la institución requieren visualizar la ocupación real garantizada de equipos sin mezclar solicitudes pendientes ni solicitudes rechazadas (requisito RF10 y CP14).
* **Decisión:** Se creó el endpoint especializado `GET /api/v1/reservas/confirmadas`:
  1. Acepta parámetros opcionales de consulta: `equipoId` (Long) y `fecha` (LocalDate ISO).
  2. En [`ReservaServiceImpl.listarConfirmadas`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L104-L121) se delega a métodos derivados en [`ReservaRepository.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/ReservaRepository.java#L25-L31) (`findByEquipoIdAndFechaAndEstado`, `findByFechaAndEstado`, etc.) fijando invariablemente `EstadoReserva.CONFIRMADA`.
  3. En el frontend se implementó el componente [`ConsultaConfirmadas.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/reserva/ConsultaConfirmadas.tsx), integrado en la pestaña administrativa de la bibliotecaria.
* **Alternativas consideradas:**
  * *Filtrar la lista global en memoria en el cliente:* Descartada por ineficiente y por transferir datos innecesarios de solicitudes pendientes.
  * *Usar el endpoint genérico `GET /api/v1/reservas?estado=CONFIRMADA`:* Descartada para proporcionar una ruta semántica clara orientada a asignaciones efectivas.
* **Consecuencias:**
  * *Positivas:* Cumplimiento de RF10 y CP14; consultas directas y optimizadas sobre reservas efectivas.
  * *Negativas:* Endpoint adicional en la API REST que complementa el contrato original.
* **Requisitos o reglas relacionadas:** RF10, CP14, REQ-005, RN-001.
* **Evidencia en el código:**
  * Controller: [`ReservaController.java:L91-L97`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L91-L97).
  * Service: [`ReservaServiceImpl.java:L104-L121`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/ReservaServiceImpl.java#L104-L121).
  * Repository: [`ReservaRepository.java:L25-L31`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/ReservaRepository.java#L25-L31).
  * Frontend: [`ConsultaConfirmadas.tsx`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/frontend/src/components/reserva/ConsultaConfirmadas.tsx).

---

## Decisiones abiertas del SSD (sección 18)

| Nº | Pregunta original del SSD | Estado | ADR de referencia |
| :---: | :--- | :---: | :---: |
| **1** | **Definición de Actores y Autorización:** ¿Cualquier docente puede confirmar o rechazar cualquier solicitud (incluidas las de otros docentes), o existe un rol administrativo / bibliotecario que deba encargarse de las confirmaciones/rechazos? | **Resuelta** | [ADR-005](#adr-005-modelo-de-roles-y-autorización-en-el-backend-spring-security) y [ADR-011](#adr-011-aislamiento-de-consultas-por-identidad-de-docente-mis-solicitudes) |
| **2** | **Estructura y catálogo de los "Módulos":** ¿Qué representa un "módulo" exactamente en el contexto institucional? ¿Es una lista fija predeterminada (ej. Módulo 1: 08:00 - 09:30, Módulo 2: 09:45 - 11:15) o se ingresa libremente? | **Resuelta** | [ADR-008](#adr-008-catálogos-de-módulos-horarios-equipos-y-docentes) |
| **3** | **Catálogo de Equipos y Docentes en el MVP:** ¿Los equipos y docentes estarán precargados mediante un script/seed de base de datos, o se ingresan como datos textuales abiertos al crear la reserva? | **Resuelta** | [ADR-008](#adr-008-catálogos-de-módulos-horarios-equipos-y-docentes) |
| **4** | **Impacto en otras solicitudes pendientes al confirmar una:** Cuando se confirma una solicitud para un equipo, fecha y módulo, ¿las demás solicitudes pendientes que competían por ese mismo slot deben rechazarse automáticamente por el sistema, o deben permanecer en estado PENDIENTE hasta que sean rechazadas manualmente? | **Resuelta** | [ADR-007](#adr-007-coexistencia-de-solicitudes-pendiente-y-persistencia-sin-rechazo-automático) |
| **5** | **Reversibilidad y motivos de rechazo:** ¿Se requiere almacenar un motivo de rechazo al pasar una solicitud a RECHAZADA? ¿Una reserva CONFIRMADA podrá cancelarse en el futuro? | **Fuera de alcance** | [ADR-009](#adr-009-exclusión-de-motivo-de-rechazo-y-cancelación-de-reservas-confirmadas) |

---

## Inconsistencias detectadas entre SSD y Código

A continuación se registran de forma explícita las diferencias comprobadas entre lo formulado en el Documento de Especificación del Sistema (SSD) y la implementación real del repositorio (el código fuente prevalece):

1. **Diferenciación de roles vs. Actor único del SSD:**
   * *SSD (Secciones 4 y 5):* Establecía únicamente al "Docente" como actor que ejecutaba todos los casos de uso (crear, ver pendientes, confirmar y rechazar).
   * *Código:* Se implementó un esquema de dos roles con Spring Security. Un docente con `ROLE_DOCENTE` **no puede** confirmar ni rechazar (recibe `HTTP 403 Forbidden`). Solo el usuario con `ROLE_BIBLIOTECARIA` (Lucía) tiene permisos para confirmar o rechazar solicitudes ([`SecurityConfig.java:L75-L76`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/config/SecurityConfig.java#L75-L76)).
2. **Métodos HTTP para Confirmar y Rechazar:**
   * *SSD (Sección 13):* Proponía indistintamente `POST` o `PATCH` (`POST /api/v1/reservas/{id}/confirmar` o `PATCH`).
   * *Código:* Se implementaron estrictamente como `POST` en [`ReservaController.java:L104, L109`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L104). No existen métodos anotados con `@PatchMapping`.
3. **Mecanismo de concurrencia para RN-001 en motor H2 de desarrollo:**
   * *SSD (Secciones 10, 11 y 17) y ARCHITECTURE.md:* Indicaban el uso de un índice único condicional o bloqueo pesimista (`SELECT FOR UPDATE`).
   * *Código:* El índice condicional parcial (`WHERE estado = 'CONFIRMADA'`) se encuentra codificado únicamente en [`schema-postgres.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema-postgres.sql#L25-L27). En el perfil activo por defecto de desarrollo H2 (`application-dev.properties` con `ddl-auto=update`), no existe un índice condicional en la base de datos ni bloqueo pesimista en [`ReservaRepository.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/repository/ReservaRepository.java), dependiendo la exclusión únicamente de la consulta lógica previa en Java.
4. **Endpoints implementados en backend no previstos en el SSD original:**
   * `GET /api/v1/reservas/mis-solicitudes`: Endpoint para aislar solicitudes del docente autenticado ([`ReservaController.java:L84-L89`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L84-L89)).
   * `GET /api/v1/reservas/confirmadas`: Endpoint para consultar asignaciones garantizadas por recurso y fecha ([`ReservaController.java:L91-L97`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L91-L97)).
   * `GET /api/v1/reservas/{id}/auditoria`: Endpoint para consultar la traza de operaciones de una reserva ([`ReservaController.java:L114-L121`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L114-L121)).
   * `GET /api/v1/equipos`: Endpoint para poblar el combo selector en el formulario ([`EquipoController.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/EquipoController.java)).
   * `GET /api/v1/health`: Endpoint de monitoreo de disponibilidad ([`HealthController.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/HealthController.java)).
5. **Alcance de Auditoría:**
   * *SSD (Sección 3):* No contemplaba registro de auditoría en los requerimientos iniciales (REQ-001 a REQ-007).
   * *Código:* Se implementó la entidad [`AuditoriaReserva.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/entity/AuditoriaReserva.java), la tabla `auditoria_reservas` y el servicio [`AuditoriaServiceImpl.java`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/service/impl/AuditoriaServiceImpl.java).
6. **Inferencia obligatoria de identidad docente en creación de solicitudes:**
   * *SSD (Sección 13.1):* Establecía que el cliente enviaba libremente `docenteId` en el payload JSON.
   * *Código:* Si el usuario que emite la petición posee `ROLE_DOCENTE`, [`ReservaController.java:L55-L58`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/java/com/biblioteca/horizonte/controller/ReservaController.java#L55-L58) sobreescribe el `docenteId` del cuerpo por el ID resuelto desde su credencial autenticada para evitar suplantaciones.
7. **Cantidad de equipos iniciales en catálogo seed:**
   * *Informe de Auditoría Técnica (RF02):* Especificaba un catálogo institucional de 6 recursos (2 proyectores y 4 notebooks).
   * *Código:* En los archivos [`data-h2.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/data-h2.sql) y [`data-postgres.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/data-postgres.sql) existen únicamente 3 equipos precargados (1 proyector, 1 notebook, 1 tablet).

---

## Elementos marcados como NO VERIFICADO

1. **Efectividad y activación del índice condicional único en PostgreSQL en ejecución:**
   * *Estado:* **NO VERIFICADO**.
   * *Motivo:* El entorno local de desarrollo opera con perfil `dev` sobre base de datos H2 en memoria. El script [`schema-postgres.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema-postgres.sql) se encuentra definido en el repositorio y verificado a nivel sintáctico, pero no se ejecutó una instancia de PostgreSQL en vivo con peticiones concurrentes para comprobar la respuesta del motor ante colisiones reales en base de datos.
2. **Compatibilidad de `schema-postgres.sql` con la entidad `AuditoriaReserva`:**
   * *Estado:* **NO VERIFICADO**.
   * *Motivo:* El archivo [`schema-postgres.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema-postgres.sql) no contiene la sentencia `CREATE TABLE auditoria_reservas` (a diferencia de [`schema.sql`](file:///c:/Users/joaqu/OneDrive/Escritorio/Biblioteca-horizonte/backend/src/main/resources/schema.sql)). Si bien Hibernate con `ddl-auto=update` crea la tabla automáticamente si se le otorgan permisos DDL, no se ha verificado la inicialización limpia en PostgreSQL cuando se utiliza exclusivamente dicho script DDL inicial.

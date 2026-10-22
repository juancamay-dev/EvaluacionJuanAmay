# EvaluacionJuanAmay — Transferencias Interbancarias

Reto técnico Full Stack: API REST de cuentas y transferencias con reglas de negocio (detección de operaciones sospechosas y límite diario), y un frontend Angular con el dashboard de cuentas y el formulario de transferencia.

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Quarkus 3.40, Quarkus REST (Jackson), Hibernate ORM con Panache, Hibernate Validator, SmallRye OpenAPI |
| Base de datos | MySQL 8.4, migraciones con Flyway |
| Frontend | Angular 20, Angular Material, Reactive Forms, signals |
| Pruebas | JUnit 5, Mockito, RestAssured, ArchUnit, Quarkus Dev Services (MySQL real); Jasmine y Karma en el frontend |
| Infraestructura | Docker y Docker Compose |

## Ejecución rápida con Docker

Requisito: Docker Desktop (o Docker Engine con Compose v2).

```bash
docker compose up --build
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/q/swagger-ui |
| Contrato OpenAPI | http://localhost:8080/q/openapi |

Al arrancar, Flyway crea el esquema y carga 4 cuentas de demostración. Para detener el entorno y borrar los datos: `docker compose down -v`.

## Ejecución local (sin Docker para backend y frontend)

Requisitos: JDK 17, Node 20 o 22 y Docker (solo para MySQL y para las pruebas).

### 1. Base de datos

Opción A, MySQL del `docker-compose` (recomendada):

```bash
docker compose up -d mysql
```

Opción B, un MySQL 8 propio. Crea la base y el usuario; las tablas las crea Flyway al iniciar el backend:

```sql
CREATE DATABASE transferencias_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'app'@'%' IDENTIFIED BY 'app123';
GRANT ALL PRIVILEGES ON transferencias_db.* TO 'app'@'%';
FLUSH PRIVILEGES;
```

Scripts de base de datos (Flyway los ejecuta automáticamente, en orden):

| Script | Contenido |
|---|---|
| `backend/src/main/resources/db/migration/V1__esquema.sql` | Tablas `cuenta` y `transferencia`, restricciones UNIQUE, CHECK, llaves foráneas e índices |
| `backend/src/main/resources/db/seed/V2__datos_demo.sql` | 4 cuentas demo (la `1000000004` está inactiva) |

Las credenciales de desarrollo (`app` / `app123`, base `transferencias_db`) son de demostración. En producción se configuran con las variables `DB_URL`, `DB_USER`, `DB_PASSWORD` y `CORS_ORIGINS`.

### 2. Backend

```bash
cd backend
./mvnw quarkus:dev            # desarrollo con recarga en caliente, http://localhost:8080
```

Compilación y ejecución del empaquetado:

```bash
./mvnw clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

### 3. Frontend

```bash
cd frontend
npm ci
npm start                     # http://localhost:4200
```

La URL de la API está en `frontend/src/environments/environment.ts` (`http://localhost:8080/api`).

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/cuentas` | Registra una cuenta | 201 (con `Location`), 400, 409 |
| GET | `/api/cuentas?pagina=0&tamano=100` | Lista las cuentas, paginadas y ordenadas por número | 200 |
| GET | `/api/cuentas/{id}` | Consulta una cuenta | 200, 404 |
| GET | `/api/cuentas/{id}/resumen` | Saldo, total transferido hoy, cantidad del día, sospechosas y disponible | 200, 404 |
| POST | `/api/transferencias` | Ejecuta una transferencia | 201, 400, 404, 422 |
| GET | `/api/transferencias?cuentaOrigen=&cuentaDestino=&desde=&hasta=&estado=&pagina=&tamano=` | Historial filtrado, ordenado por fecha descendente | 200, 400 |

Ejemplo:

```bash
curl -X POST localhost:8080/api/transferencias -H 'Content-Type: application/json' \
  -d '{"cuentaOrigen":"1000000001","cuentaDestino":"1000000002","monto":250.00}'
```

Todos los errores comparten el mismo formato:

```json
{
  "timestamp": "2026-10-21T15:00:00Z",
  "status": 422,
  "codigo": "LIMITE_DIARIO_EXCEDIDO",
  "mensaje": "La transferencia supera el límite diario de $10,000.00. Monto disponible hoy: $1,000.00",
  "detalles": ["transferenciaId: 15"]
}
```

| Código | Status | Cuándo |
|---|---|---|
| `VALIDACION` | 400 | Campos inválidos; `detalles` lista `campo: mensaje` |
| `JSON_INVALIDO` | 400 | Cuerpo mal formado o valor de enum desconocido |
| `SOLICITUD_INVALIDA` | 400 | Misma cuenta origen y destino, filtros de historial inválidos |
| `RECURSO_NO_ENCONTRADO` | 404 | Cuenta inexistente |
| `CUENTA_DUPLICADA` | 409 | Número de cuenta ya registrado |
| `CUENTA_INACTIVA`, `SALDO_INSUFICIENTE`, `LIMITE_DIARIO_EXCEDIDO` | 422 | Rechazos de negocio; la transferencia queda guardada como `RECHAZADA` |

## Reglas de negocio y supuestos

- **Cuenta:** número único de exactamente 10 dígitos, saldo inicial ≥ 0, tipo `AHORRO`/`CORRIENTE` y estado `ACTIVA`/`INACTIVA` (se aceptan en minúsculas).
- **Transferencia:** monto > 0 con máximo 2 decimales; origen y destino distintos, existentes y activos; saldo suficiente (se permite transferir el saldo exacto).
- **Operación sospechosa (CA-03):** el monto supera $5,000.00 ($5,000.00 exactos no es sospechoso), o la cuenta origen ya hizo 5 transferencias en la última hora (la actual sería la sexta). La transferencia **se ejecuta** y queda `EN_REVISION` con el motivo; si se cumplen ambas condiciones se registran ambos motivos.
- **Límite diario (CA-04):** $10,000.00 acumulados por cuenta origen en el **día calendario de Ecuador** (`America/Guayaquil`), no de UTC. Cuentan las transferencias `COMPLETADA` y `EN_REVISION`; las `RECHAZADA` no. Un monto igual al disponible se permite.
- **Rechazos auditables:** cuenta inactiva, saldo insuficiente y límite excedido responden 422 y quedan registrados como `RECHAZADA`, sin mover saldos.
- **Historial (CA-05):** `desde` y `hasta` son fechas locales inclusivas (`yyyy-MM-dd`); el estado acepta mayúsculas o minúsculas.
- **Resumen (CA-05):** "cantidad de transferencias del día" son las enviadas y realizadas por la cuenta; "sospechosas" se reporta del día y en total.
- Los umbrales son configurables en `application.properties` (`app.reglas.*`, `app.zona-horaria`).

## Decisiones técnicas

- **Arquitectura por capas y por módulo** (`cuenta`, `transferencia`, `shared`): `api` (resources) → `application` (servicios, DTOs, mappers) → `domain` (entidades) e `infrastructure` (repositorios). Los resources solo delegan, la lógica de negocio vive en los servicios y los repositorios solo acceden a datos. Una prueba de **ArchUnit** hace fallar el build si alguna dependencia rompe esa dirección, si un resource usa una entidad o si hay inyección por campo.
- **Atomicidad y concurrencia:** cada transferencia es una transacción `@Transactional`. Ambas cuentas se bloquean con `SELECT ... FOR UPDATE`, siempre en orden ascendente de número, lo que evita el doble gasto y los deadlocks entre transferencias cruzadas.
- **Rechazos sin rollback:** `@Transactional(dontRollbackOn = TransferenciaRechazadaException.class)` guarda la transferencia `RECHAZADA` en la misma transacción. No se usa `REQUIRES_NEW`, porque esa nueva transacción esperaría el bloqueo de la externa por la llave foránea.
- **Dinero y tiempo:** `BigDecimal` con `DECIMAL(15,2)`; fechas como `Instant` en UTC. El día calendario se calcula con un `Clock` inyectado (ningún `Instant.now()` en la lógica), lo que permite fijar el tiempo en las pruebas.
- **Errores centralizados:** `ExceptionMapper` para negocio, validación y errores genéricos. Los errores inesperados se registran en el log y responden 500 con un mensaje neutro, sin stacktrace.
- **DTOs y mapeo manual:** las entidades JPA nunca salen de los servicios. La inyección de dependencias es por constructor con `@Inject`.
- **Logs estructurados** (JBoss Logging, el estándar de Quarkus) en formato `evento clave=valor`: `transferencia.ejecutada`, `transferencia.rechazada`, `fraude.sospecha`, `cuenta.creada`.
- **Defensa en profundidad:** la base de datos repite las reglas críticas con UNIQUE y CHECK (saldo ≥ 0, monto > 0).
- **Frontend:** componentes standalone con signals y `computed` para el orden por saldo; un único `extraerMensajeError` traduce los errores HTTP; el formulario usa un validador de grupo para origen ≠ destino.

## Estrategia de pruebas

El backend se construyó con TDD. La pirámide de pruebas, de la base a la cima:

1. **Dominio** (JUnit puro): invariantes de `Cuenta` y `Transferencia`, `RangoDia` y `ResultadoDeteccion`.
2. **Servicios** (Mockito y `Clock.fixed`): límite diario, detección de sospechosas, transferencias (incluido el orden de bloqueo con `InOrder`), cuentas, historial y resumen.
3. **Integración** (`@QuarkusTest` con RestAssured contra **MySQL 8.4 real** levantado por Dev Services): repositorios y endpoints, incluido que la transferencia rechazada queda persistida.
4. **Concurrencia:** 10 transferencias simultáneas de $200 sobre $1,000 dan exactamente 5 aprobadas y 5 rechazadas, con saldo final 0; transferencias cruzadas A↔B en paralelo terminan sin deadlock y conservan la suma de saldos.
5. **Contrato y arquitectura:** `OpenApiTest` valida paths, resúmenes, códigos y ejemplos; `ArquitecturaTest` valida las capas.
6. **Frontend** (Jasmine y Karma con `HttpTestingController`): servicios, traducción de errores, dashboard (render, orden por saldo, error, vacío, recarga), formulario (validaciones, envío, mensajes, botón deshabilitado) y composición.

Resultados: 114 pruebas del backend y 26 del frontend, todas en verde.

```bash
# Backend: requiere Docker en ejecución (Dev Services levanta un MySQL desechable)
cd backend && ./mvnw verify          # reporte de cobertura: backend/target/jacoco-report/index.html

# Frontend
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless
```

## Mejoras futuras

- Autenticación y autorización con OIDC (Keycloak) y cuentas por usuario.
- Cabecera `Idempotency-Key` en `POST /api/transferencias` para reintentos seguros.
- Patrón outbox y Kafka para notificar las operaciones sospechosas a un equipo antifraude.
- Métricas y trazas con Micrometer y OpenTelemetry.
- Historial de transferencias y resumen de cuenta en el frontend.
- Pipeline de CI (GitHub Actions) con las suites de backend y frontend.

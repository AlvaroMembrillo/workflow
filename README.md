# workflow

[![Backend CI](https://github.com/AlvaroMembrillo/workflow/actions/workflows/backend.yml/badge.svg)](https://github.com/AlvaroMembrillo/workflow/actions/workflows/backend.yml)
[![Frontend CI](https://github.com/AlvaroMembrillo/workflow/actions/workflows/frontend.yml/badge.svg)](https://github.com/AlvaroMembrillo/workflow/actions/workflows/frontend.yml)
[![E2E](https://github.com/AlvaroMembrillo/workflow/actions/workflows/e2e.yml/badge.svg)](https://github.com/AlvaroMembrillo/workflow/actions/workflows/e2e.yml)

Portal de empleo desarrollado con Spring Boot y Angular. Todas las ofertas indican el salario y cada
candidato ve en qué punto está su candidatura, con la fecha de cada paso.

## Pruébalo en un comando

Solo necesitas Docker:

```bash
docker compose -f docker-compose.demo.yml up --build
```

Abre http://localhost:8000. La demo arranca con 6 empresas, 15 ofertas y candidaturas en todos los
estados. Estas cuentas tienen la contraseña `demo-workflow`:

| Cuenta | Tipo | Qué ver |
|---|---|---|
| `ana@demo.test` | Candidata | Mis candidaturas: una en revisión, una seleccionada, una no seleccionada y una enviada |
| `rrhh@lumen.test` | Empresa | Panel de Lumen Seguros: la oferta de Java tiene candidaturas pendientes, una con 7 días de espera |
| `luis@demo.test` | Candidato | Una candidatura retirada |

La documentación de la API está en http://localhost:8000/swagger-ui.html. Para borrar los datos de la
demo: `docker compose -f docker-compose.demo.yml down -v`.

## Stack

- **Backend:** Java 25, Spring Boot 4, Spring Security (OAuth2 Resource Server), Spring Data JPA, Flyway, MapStruct
- **Frontend:** Angular 22 sin zone.js (signals), formularios reactivos, CSS con variables propias
- **Base de datos:** PostgreSQL 17
- **Tests:** JUnit 5, MockMvc y Testcontainers en el backend; Vitest en el frontend; Playwright y axe-core de extremo a extremo
- **Despliegue:** imágenes Docker multietapa (JRE 25 sin root; nginx con CSP) y Docker Compose

## Desarrollo

Requisitos: Java 25, Node 24 y Docker.

```bash
docker compose up -d                        # PostgreSQL
cd backend && ./mvnw spring-boot:run        # API en http://localhost:8080
cd frontend && npm install && npm start     # Web en http://localhost:4200
```

- Web: http://localhost:4200 (reenvía `/api` al backend, sin configurar CORS)
- Swagger UI: http://localhost:8080/swagger-ui.html

Tests del backend (levantan su propio PostgreSQL con Testcontainers): `./mvnw test`.
Tests del frontend: `npm test`. Más detalles en [frontend/README.md](frontend/README.md).

### Tests de extremo a extremo

Recorren la aplicación en un navegador real contra la demo en Docker: la búsqueda (también en móvil), el
recorrido completo de un candidato (registro, inscripción, seguimiento y retirada), el de una empresa
(revisar y aceptar una candidatura, publicar una oferta) y una auditoría de accesibilidad WCAG 2.2 AA con
axe-core en tema claro y oscuro. Crean sus propios usuarios, así que se pueden repetir.

```bash
docker compose -f docker-compose.demo.yml up --build -d --wait
cd e2e && npm install && npx playwright install chromium && npm test
```

## API

Hay dos tipos de cuenta: **candidatos**, que se inscriben en ofertas, y **empresas**, que las publican y
gestionan las candidaturas que reciben. La documentación completa está en Swagger UI.

| Endpoint | Acceso | Descripción |
|---|---|---|
| `GET /api/ofertas` | Público | Ofertas abiertas, paginadas, con filtros `texto`, `ubicacion`, `modalidad`, `tipoContrato` y `empresaId` |
| `GET /api/ofertas/{id}` | Público | Detalle de una oferta |
| `POST /api/ofertas` | Empresa | Publica una oferta (el rango salarial es obligatorio) |
| `PUT /api/ofertas/{id}` | Empresa propietaria | Modifica una oferta |
| `PATCH /api/ofertas/{id}/estado` | Empresa propietaria | Abre o cierra una oferta |
| `GET /api/empresas/{id}` | Público | Perfil de una empresa |
| `GET` y `PUT /api/empresas/me` | Empresa | Perfil de la empresa propia |
| `GET /api/empresas/me/ofertas` | Empresa | Todas sus ofertas, también las cerradas, con cuántas candidaturas tiene cada una en cada estado |
| `POST /api/ofertas/{id}/candidaturas` | Candidato | Se inscribe en una oferta abierta |
| `GET /api/candidaturas/me` | Candidato | Sus candidaturas, el estado de cada una y la fecha de cada paso |
| `GET /api/ofertas/{id}/mi-candidatura` | Candidato | Su candidatura en una oferta, si se ha inscrito |
| `POST /api/candidaturas/{id}/retirada` | Candidato propietario | Retira la candidatura mientras no haya decisión |
| `GET /api/ofertas/{id}/candidaturas` | Empresa propietaria | Candidaturas recibidas en una oferta, con filtro opcional `estado` |
| `PATCH /api/candidaturas/{id}/estado` | Empresa propietaria | `PENDIENTE` → `EN_REVISION` → `ACEPTADA` o `RECHAZADA` |

Las ofertas abiertas que llevan 60 días sin cambios se cierran solas cada noche, para que el listado no
acumule vacantes que ya no están activas.

Los errores siguen el formato [Problem Details (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457), con el
detalle de cada campo cuando falla la validación.

## Autenticación

La API usa **JWT emitidos por el propio backend**. El usuario se registra o inicia sesión, recibe un token
firmado y lo envía en cada petición con la cabecera `Authorization: Bearer <token>`.

| Endpoint | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/registro` | Público | Crea una cuenta de `CANDIDATO` o `EMPRESA` y devuelve un token |
| `POST /api/auth/login` | Público | Devuelve un token si el email y la contraseña son correctos |
| `GET /api/usuarios/me` | Token | Datos del usuario autenticado |

Para probarlo desde Swagger UI: llama a `/api/auth/login`, copia el `accessToken` y pégalo en **Authorize**.

### Decisiones

- **JWT propio en lugar de Keycloak o Auth0.** El proyecto se puede ejecutar sin cuentas ni servicios
  externos, y toda la lógica de seguridad queda visible en el código. La validación de tokens usa el
  OAuth2 Resource Server estándar de Spring, así que migrar a un proveedor externo sería sobre todo
  cambiar configuración (`issuer-uri`).
- **Firma RS256 con claves RSA.** Solo el backend tiene la clave privada; la pública podría publicarse para
  que otros servicios validen los tokens sin poder emitirlos.
- **Sin estado.** No hay sesión en el servidor. Como el token viaja en una cabecera y no en una cookie,
  CSRF no aplica.
- **Contraseñas con BCrypt** mediante `DelegatingPasswordEncoder`, que guarda el algoritmo junto al hash
  para poder cambiarlo en el futuro sin invalidar las contraseñas existentes.
- **Sin pistas para atacantes:** el login responde igual si el email no existe o si la contraseña es
  incorrecta, y el registro no permite crear cuentas `ADMIN`.
- **Roles en el token** (`roles`), convertidos en authorities `ROLE_*` para usar `@PreAuthorize("hasRole('EMPRESA')")`.

### Clave de firma

En el perfil `dev` la clave privada se genera sola la primera vez en `backend/.jwt/` (ignorada por git).
En cualquier otro entorno hay que indicar un fichero PEM (PKCS#8) con la variable `JWT_CLAVE_PRIVADA`;
si no existe, la aplicación no arranca. Para generarlo:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out clave-privada.pem
```

Otras variables: `CORS_ORIGENES_PERMITIDOS` (por defecto `http://localhost:4200`).

### Siguientes pasos

- Tokens de refresco en cookie `HttpOnly`, para sesiones largas sin alargar la vida del token de acceso

# workflow
Portal de empleo desarrollado con Spring Boot y Angular.

## Stack

- **Backend:** Java 25, Spring Boot 4, Spring Security (OAuth2 Resource Server), Spring Data JPA, Flyway, MapStruct
- **Base de datos:** PostgreSQL 17
- **Tests:** JUnit 5, MockMvc, Testcontainers
- **Frontend:** Angular (pendiente)

## Cómo arrancarlo

Requisitos: Java 25 y Docker.

```bash
docker compose up -d                # PostgreSQL
cd backend && ./mvnw spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

Para ejecutar los tests (levantan su propio PostgreSQL con Testcontainers): `./mvnw test`

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

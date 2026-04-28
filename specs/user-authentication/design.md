# Diseño Técnico: User Authentication

## 1. Arquitectura propuesta

El módulo `auth` sigue la arquitectura **modular monolítica** establecida por Spring Modulith. Se organiza en capas internas (api → application → domain ← infrastructure) con separación clara de responsabilidades.

```
Petición HTTP
     │
     ▼
[JwtAuthFilter]          ← Valida access token en cada request protegido
     │
     ▼
[AuthController]         ← Recibe DTOs, delega a servicios
     │
     ▼
[AuthService]            ← Orquesta flujos de registro, login, etc.
[TokenService]           ← Genera/valida JWT y refresh tokens
[EmailService]           ← Envía emails (verificación, reset)
     │
     ▼
[UserRepository]         ← JPA → PostgreSQL
[RefreshTokenRepository]
[VerificationTokenRepository]
[PasswordResetTokenRepository]
```

**Flujo OAuth2:**
```
Browser → GET /api/auth/oauth2/google
        → Spring Security → Google
        → Callback /login/oauth2/code/google
        → OAuth2SuccessHandler → genera JWT → redirige al frontend con tokens
```

---

## 2. Estructura de módulos

```
src/main/java/edward/com/finanzasbackend/
└── auth/                                  ← módulo Spring Modulith
    ├── api/
    │   ├── AuthController.java
    │   └── dto/
    │       ├── RegisterRequest.java
    │       ├── LoginRequest.java
    │       ├── AuthResponse.java          ← { accessToken, refreshToken, expiresIn }
    │       ├── RefreshRequest.java
    │       ├── LogoutRequest.java
    │       ├── ResendVerificationRequest.java
    │       ├── ForgotPasswordRequest.java
    │       └── ResetPasswordRequest.java
    ├── application/
    │   ├── AuthService.java               ← casos de uso: register, login, logout, etc.
    │   ├── TokenService.java              ← genera/valida JWT y refresh tokens
    │   └── EmailService.java             ← envío de emails transaccionales
    ├── domain/
    │   ├── User.java                      ← @Entity principal
    │   ├── UserStatus.java               ← enum: PENDING_VERIFICATION, ACTIVE
    │   ├── RefreshToken.java             ← @Entity
    │   ├── VerificationToken.java        ← @Entity
    │   └── PasswordResetToken.java       ← @Entity
    ├── infrastructure/
    │   ├── UserRepository.java           ← JpaRepository<User, UUID>
    │   ├── RefreshTokenRepository.java
    │   ├── VerificationTokenRepository.java
    │   └── PasswordResetTokenRepository.java
    └── security/
        ├── SecurityConfig.java           ← filter chain, rutas públicas/protegidas
        ├── JwtAuthFilter.java            ← OncePerRequestFilter para validar JWT
        └── OAuth2SuccessHandler.java     ← emite JWT tras login con Google
```

---

## 3. Esquema de base de datos

```sql
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),          -- NULL para usuarios OAuth-only
    status        VARCHAR(30)  NOT NULL, -- PENDING_VERIFICATION | ACTIVE
    google_id     VARCHAR(255),          -- NULL para usuarios locales
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE verification_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token      VARCHAR(255) UNIQUE NOT NULL,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token      VARCHAR(255) UNIQUE NOT NULL,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE password_reset_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token      VARCHAR(255) UNIQUE NOT NULL,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

> Hibernate generará las tablas con `ddl-auto: validate` en producción y `create-drop` en desarrollo. Las migraciones definitivas irán con Flyway (fuera de este scope).

---

## 4. Contratos de datos (API)

### Endpoints públicos (`/api/auth/**`)

| Método | Path | Request body | Response |
|--------|------|-------------|----------|
| POST | `/api/auth/register` | `{ name, email, password }` | `201 { userId }` |
| GET | `/api/auth/verify-email` | `?token=...` | `200 { message }` |
| POST | `/api/auth/resend-verification` | `{ email }` | `200 { message }` |
| POST | `/api/auth/login` | `{ email, password }` | `200 AuthResponse` |
| GET | `/api/auth/oauth2/google` | — | redirect a Google |
| POST | `/api/auth/refresh` | `{ refreshToken }` | `200 { accessToken, expiresIn }` |
| POST | `/api/auth/logout` | `{ refreshToken }` | `204` |
| POST | `/api/auth/forgot-password` | `{ email }` | `200 { message }` |
| POST | `/api/auth/reset-password` | `{ token, newPassword }` | `200 { message }` |

### DTOs clave

```java
// AuthResponse
record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn        // segundos: 900
) {}

// RegisterRequest
record RegisterRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @Size(min = 8) String password
) {}

// LoginRequest
record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {}
```

### Estructura JWT (claims del access token)

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "iat": 1714089600,
  "exp": 1714090500
}
```

---

## 5. Servicios — responsabilidades

### `AuthService`
- `register(RegisterRequest)` → crea usuario `PENDING_VERIFICATION`, genera `VerificationToken`, dispara email
- `verifyEmail(token)` → activa usuario, marca token como `used`
- `resendVerification(email)` → invalida token anterior, genera nuevo, reenvía email
- `login(LoginRequest)` → valida credenciales, verifica estado `ACTIVE`, genera par de tokens
- `refreshToken(refreshToken)` → valida refresh token en BD, emite nuevo access token
- `logout(refreshToken)` → marca refresh token como `revoked`
- `forgotPassword(email)` → genera `PasswordResetToken`, envía email (siempre retorna OK)
- `resetPassword(token, newPassword)` → valida token, actualiza hash, revoca todos los refresh tokens del usuario

### `TokenService`
- `generateAccessToken(User)` → JWT HS256, 15 min, firmado con `app.jwt.secret`
- `validateAccessToken(token)` → parsea y valida JWT, retorna claims
- `generateRefreshToken(User)` → UUID aleatorio, persiste en BD con TTL 7 días
- `revokeAllRefreshTokens(userId)` → usado al resetear contraseña

### `EmailService`
- `sendVerificationEmail(user, token)` → usa Spring Mail + plantilla
- `sendPasswordResetEmail(user, token)` → usa Spring Mail + plantilla

---

## 6. Configuración de Spring Security

```
SecurityFilterChain:
  - CSRF deshabilitado (API stateless)
  - SessionManagement: STATELESS
  - Rutas públicas: /api/auth/**, /login/oauth2/**, /error
  - Resto: autenticado (JWT requerido)
  - JwtAuthFilter antes de UsernamePasswordAuthenticationFilter
  - OAuth2Login: habilitado con OAuth2SuccessHandler personalizado
```

**`OAuth2SuccessHandler`**: al completar el flujo de Google, extrae el email del `OAuth2User`, busca o crea el `User` en BD, emite JWT y redirige al frontend con los tokens como query params (o en header, según el frontend defina).

---

## 7. Dependencias a agregar en `pom.xml`

```xml
<!-- Web -->
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-validation</dependency>

<!-- Seguridad -->
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-oauth2-client</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Persistencia -->
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Email -->
<dependency>spring-boot-starter-mail</dependency>

<!-- Tests -->
<dependency>spring-security-test</dependency>  <!-- scope: test -->
```

---

## 8. Propiedades de configuración (`application.yaml`)

```yaml
spring:
  application:
    name: finanzasBackend
  datasource:
    url: jdbc:postgresql://localhost:5432/finanzas_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: create-drop   # dev; cambiar a validate en prod
    show-sql: false
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USER}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile

app:
  jwt:
    secret: ${JWT_SECRET}          # mínimo 256 bits para HS256
    access-token-expiration: 900   # segundos (15 min)
    refresh-token-expiration: 604800  # segundos (7 días)
  frontend-url: http://localhost:3000   # para redirect OAuth2 y links de email
```

---

## 9. Dependencias y riesgos

### Dependencias externas
| Dependencia | Propósito | Acción requerida |
|------------|-----------|-----------------|
| PostgreSQL | Base de datos | Instalar localmente o usar Docker |
| Gmail SMTP / otro | Envío de emails | Configurar credenciales SMTP |
| Google Cloud Console | OAuth2 credentials | Crear proyecto + OAuth2 client ID |

### Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|-----------|
| Spring Boot 4.x con JJWT 0.12.6 — compatibilidad no verificada | Medio | Verificar en primera compilación; alternativa: usar `spring-boot-starter-oauth2-resource-server` con Nimbus |
| Redirect URI de OAuth2 debe coincidir con Google Console | Alto | Documentar URI exacta: `http://localhost:8080/login/oauth2/code/google` |
| Tokens en query params (OAuth2 redirect) expuestos en logs del servidor | Medio | Mover a fragmento de URL o usar cookie HttpOnly en el frontend |
| Java 17 en pom.xml vs Java 21 en CLAUDE.md | Bajo | Actualizar `<java.version>21</java.version>` en pom.xml antes de implementar |
| Spring Modulith: el módulo `auth` no debe depender de módulos de negocio | Bajo | Respetar la dirección de dependencias: business → auth (via eventos) |

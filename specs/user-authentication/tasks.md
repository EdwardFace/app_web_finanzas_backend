# Tareas de implementación: User Authentication

> Orden de ejecución sugerido: Setup → Domain → Infrastructure → DTOs → Exceptions → Services → Security → Controller → Tests

---

## SETUP

- [x] **T-01 — Actualizar versión de Java a 21 en `pom.xml`**
  - Cambiar `<java.version>17</java.version>` → `<java.version>21</java.version>`
  - **Hecho cuando:** `mvn compile` pasa sin errores de versión.
  - **Archivos:** `pom.xml`

- [x] **T-02 — Agregar dependencias requeridas en `pom.xml`**
  - Agregar: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, `jjwt-api:0.12.6`, `jjwt-impl:0.12.6` (runtime), `jjwt-jackson:0.12.6` (runtime), `spring-boot-starter-data-jpa`, `postgresql` (runtime), `spring-boot-starter-mail`, `spring-security-test` (test).
  - **Hecho cuando:** `mvn dependency:resolve` resuelve todas las dependencias sin conflictos.
  - **Archivos:** `pom.xml`

- [x] **T-03 — Configurar `application.yaml`**
  - Agregar secciones: `datasource` (PostgreSQL), `jpa` (ddl-auto: create-drop), `mail` (SMTP), `security.oauth2.client` (Google), `app.jwt` (secret, expirations), `app.frontend-url`.
  - Usar variables de entorno para todos los secretos (`${DB_USER}`, `${JWT_SECRET}`, etc.).
  - **Hecho cuando:** La aplicación levanta sin errores de binding de propiedades (con las env vars seteadas).
  - **Archivos:** `src/main/resources/application.yaml`

---

## DOMAIN

- [x] **T-04 — Crear enum `UserStatus`**
  - Valores: `PENDING_VERIFICATION`, `ACTIVE`.
  - **Hecho cuando:** El enum compila y puede usarse como tipo en `User`.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/domain/UserStatus.java`

- [x] **T-05 — Crear entidad `User`**
  - Campos: `id` (UUID, generado), `name`, `email` (unique), `passwordHash` (nullable), `status` (UserStatus), `googleId` (nullable), `createdAt`, `updatedAt`.
  - Anotaciones JPA: `@Entity`, `@Table(name="users")`, `@PrePersist`/`@PreUpdate` para timestamps.
  - **Hecho cuando:** Hibernate genera la tabla `users` al arrancar en modo create-drop sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/domain/User.java`

- [x] **T-06 — Crear entidad `VerificationToken`**
  - Campos: `id` (UUID), `token` (unique), `user` (@ManyToOne → User), `expiresAt`, `used` (boolean, default false), `createdAt`.
  - **Hecho cuando:** Hibernate genera tabla `verification_tokens` sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/domain/VerificationToken.java`

- [x] **T-07 — Crear entidad `RefreshToken`**
  - Campos: `id` (UUID), `token` (unique), `user` (@ManyToOne → User), `expiresAt`, `revoked` (boolean, default false), `createdAt`.
  - **Hecho cuando:** Hibernate genera tabla `refresh_tokens` sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/domain/RefreshToken.java`

- [x] **T-08 — Crear entidad `PasswordResetToken`**
  - Campos: `id` (UUID), `token` (unique), `user` (@ManyToOne → User), `expiresAt`, `used` (boolean, default false), `createdAt`.
  - **Hecho cuando:** Hibernate genera tabla `password_reset_tokens` sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/domain/PasswordResetToken.java`

---

## INFRASTRUCTURE

- [x] **T-09 — Crear `UserRepository`**
  - Extends `JpaRepository<User, UUID>`.
  - Métodos: `findByEmail(String email): Optional<User>`, `existsByEmail(String email): boolean`, `findByGoogleId(String googleId): Optional<User>`.
  - **Hecho cuando:** Spring Data genera la implementación sin errores al levantar el contexto.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/infrastructure/UserRepository.java`

- [x] **T-10 — Crear `VerificationTokenRepository`**
  - Extends `JpaRepository<VerificationToken, UUID>`.
  - Métodos: `findByToken(String token): Optional<VerificationToken>`, `deleteByUser(User user)`.
  - **Hecho cuando:** Compila y el contexto Spring levanta sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/infrastructure/VerificationTokenRepository.java`

- [x] **T-11 — Crear `RefreshTokenRepository`**
  - Extends `JpaRepository<RefreshToken, UUID>`.
  - Métodos: `findByToken(String token): Optional<RefreshToken>`, `revokeAllByUser(User user)` (query `@Modifying @Query`).
  - **Hecho cuando:** Compila y el contexto Spring levanta sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/infrastructure/RefreshTokenRepository.java`

- [x] **T-12 — Crear `PasswordResetTokenRepository`**
  - Extends `JpaRepository<PasswordResetToken, UUID>`.
  - Métodos: `findByToken(String token): Optional<PasswordResetToken>`, `deleteByUser(User user)`.
  - **Hecho cuando:** Compila y el contexto Spring levanta sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/infrastructure/PasswordResetTokenRepository.java`

---

## DTOs

- [x] **T-13 — Crear todos los DTOs de request y response**
  - Crear como Java records con validaciones Bean Validation:
    - `RegisterRequest(name, email, password)` — `@NotBlank`, `@Email`, `@Size(min=8)`
    - `LoginRequest(email, password)` — `@Email`, `@NotBlank`
    - `AuthResponse(accessToken, refreshToken, expiresIn)`
    - `RefreshRequest(refreshToken)` — `@NotBlank`
    - `LogoutRequest(refreshToken)` — `@NotBlank`
    - `ResendVerificationRequest(email)` — `@Email`
    - `ForgotPasswordRequest(email)` — `@Email`
    - `ResetPasswordRequest(token, newPassword)` — `@NotBlank`, `@Size(min=8)`
    - `MessageResponse(message)` — respuesta genérica de texto
  - **Hecho cuando:** Todos los records compilan sin errores.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/api/dto/*.java` (9 archivos)

---

## MANEJO DE EXCEPCIONES

- [x] **T-14 — Crear excepciones de dominio y `GlobalExceptionHandler`**
  - Excepciones custom (extends `RuntimeException`):
    - `EmailAlreadyExistsException` → `409 Conflict`
    - `InvalidTokenException` → `400 Bad Request`
    - `EmailNotVerifiedException` → `403 Forbidden`
    - `InvalidCredentialsException` → `401 Unauthorized`
    - `UserNotFoundException` → `404 Not Found`
  - `GlobalExceptionHandler` con `@RestControllerAdvice` que mapea cada excepción a su HTTP status + `{ error: mensaje }`.
  - **Hecho cuando:** Un test unitario del handler verifica que cada excepción retorna el status correcto.
  - **Archivos:**
    - `src/main/java/edward/com/finanzasbackend/auth/domain/exception/*.java` (5 archivos)
    - `src/main/java/edward/com/finanzasbackend/auth/api/GlobalExceptionHandler.java`

---

## SERVICES

- [x] **T-15 — Implementar `TokenService`**
  - `generateAccessToken(User)`: genera JWT HS256 con claims `sub` (userId), `email`, exp 15 min; usa `app.jwt.secret` inyectado vía `@Value`.
  - `validateAccessToken(String)`: parsea JWT, lanza `InvalidTokenException` si es inválido/expirado; retorna claims.
  - `generateRefreshToken(User)`: genera UUID, persiste `RefreshToken` en BD con TTL 7 días, retorna el string del token.
  - `revokeAllRefreshTokens(UUID userId)`: llama a `RefreshTokenRepository.revokeAllByUser`.
  - **Hecho cuando:** Test unitario verifica generación y validación de access token con token válido e inválido.
  - **Archivos:**
    - `src/main/java/edward/com/finanzasbackend/auth/application/TokenService.java`
    - `src/test/java/edward/com/finanzasbackend/auth/application/TokenServiceTest.java`

- [x] **T-16 — Implementar `EmailService`**
  - `sendVerificationEmail(User user, String token)`: envía email con enlace `{frontend-url}/verify-email?token={token}` usando `JavaMailSender`.
  - `sendPasswordResetEmail(User user, String token)`: envía email con enlace `{frontend-url}/reset-password?token={token}`.
  - Asunto y cuerpo en texto plano (HTML en iteraciones futuras).
  - **Hecho cuando:** Test unitario con `JavaMailSender` mockeado verifica que se llama `send()` con los parámetros correctos.
  - **Archivos:**
    - `src/main/java/edward/com/finanzasbackend/auth/application/EmailService.java`
    - `src/test/java/edward/com/finanzasbackend/auth/application/EmailServiceTest.java`

- [x] **T-17 — Implementar `AuthService`**
  - Implementar los 8 métodos definidos en el diseño: `register`, `verifyEmail`, `resendVerification`, `login`, `refreshToken`, `logout`, `forgotPassword`, `resetPassword`.
  - Cada método lanza la excepción de dominio correspondiente ante flujos de error.
  - `resetPassword` debe llamar a `tokenService.revokeAllRefreshTokens(userId)`.
  - **Hecho cuando:** Tests unitarios con repositorios mockeados cubren el camino feliz y los casos de error de cada método.
  - **Archivos:**
    - `src/main/java/edward/com/finanzasbackend/auth/application/AuthService.java`
    - `src/test/java/edward/com/finanzasbackend/auth/application/AuthServiceTest.java`

---

## SECURITY

- [x] **T-18 — Implementar `JwtAuthFilter`**
  - Extiende `OncePerRequestFilter`.
  - Extrae el header `Authorization: Bearer <token>`, valida con `TokenService.validateAccessToken`, carga el `UsernamePasswordAuthenticationToken` en el `SecurityContextHolder`.
  - Si el token falta o es inválido, deja pasar la request sin autenticación (la cadena de filtros bloqueará si el endpoint lo requiere).
  - **Hecho cuando:** Test unitario verifica que una request con token válido popula el `SecurityContext` y una con token inválido no lo hace.
  - **Archivos:**
    - `src/main/java/edward/com/finanzasbackend/auth/security/JwtAuthFilter.java`
    - `src/test/java/edward/com/finanzasbackend/auth/security/JwtAuthFilterTest.java`

- [x] **T-19 — Implementar `OAuth2SuccessHandler`**
  - Extiende `SimpleUrlAuthenticationSuccessHandler`.
  - Al éxito del flujo OAuth2: extrae email del `OAuth2User`, llama a `UserRepository.findByEmail` → si no existe, crea usuario `ACTIVE` con `googleId`; si existe sin `googleId`, lo vincula.
  - Genera par de tokens con `TokenService`, redirige a `{frontend-url}/oauth2/callback?accessToken=...&refreshToken=...`.
  - **Hecho cuando:** Test unitario con mocks verifica creación de usuario nuevo y vinculación de existente.
  - **Archivos:**
    - `src/main/java/edward/com/finanzasbackend/auth/security/OAuth2SuccessHandler.java`
    - `src/test/java/edward/com/finanzasbackend/auth/security/OAuth2SuccessHandlerTest.java`

- [x] **T-20 — Implementar `SecurityConfig`**
  - `@Configuration @EnableWebSecurity`.
  - Define `SecurityFilterChain`: CSRF off, sesión STATELESS, rutas públicas (`/api/auth/**`, `/login/oauth2/**`, `/error`), resto autenticado.
  - Registra `JwtAuthFilter` antes de `UsernamePasswordAuthenticationFilter`.
  - Habilita `oauth2Login` con `successHandler(oauth2SuccessHandler)`.
  - Expone bean `PasswordEncoder` (BCrypt).
  - **Hecho cuando:** `mvn spring-boot:run` levanta sin errores y `GET /api/auth/login` (sin token) retorna `401`, no `403`.
  - **Archivos:** `src/main/java/edward/com/finanzasbackend/auth/security/SecurityConfig.java`

---

## CONTROLLER

- [x] **T-21 — Implementar `AuthController`**
  - `@RestController @RequestMapping("/api/auth")`.
  - Mapear los 9 endpoints del diseño, delegando completamente a `AuthService`.
  - Usar `@Valid` en todos los request bodies.
  - Retornar `ResponseEntity` con el status HTTP correcto según el contrato (201, 200, 204).
  - **Hecho cuando:** Tests de integración con `@WebMvcTest` + `AuthService` mockeado verifican status HTTP y estructura del body para cada endpoint.
  - **Archivos:**
    - `src/main/java/edward/com/finanzasbackend/auth/api/AuthController.java`
    - `src/test/java/edward/com/finanzasbackend/auth/api/AuthControllerTest.java`

---

## TESTS DE INTEGRACIÓN

- [x] **T-22 — Test de integración end-to-end: flujo de registro y verificación**
  - `@SpringBootTest` + base de datos H2 o Testcontainers PostgreSQL.
  - Escenario 1: registro exitoso → usuario queda `PENDING_VERIFICATION` → verificación del email → usuario queda `ACTIVE`.
  - Escenario 2: registro con email duplicado → `409`.
  - Escenario 3: verificación con token expirado → `400`.
  - **Hecho cuando:** Los 3 escenarios pasan en `mvn test`.
  - **Archivos:** `src/test/java/edward/com/finanzasbackend/auth/RegisterAndVerifyIT.java`

- [x] **T-23 — Test de integración end-to-end: flujo de login y tokens**
  - Escenario 1: login exitoso → recibe `accessToken` + `refreshToken`.
  - Escenario 2: login con cuenta no verificada → `403`.
  - Escenario 3: refresh con token válido → nuevo `accessToken`.
  - Escenario 4: logout → refresh token revocado → intento de refresh retorna `401`.
  - **Hecho cuando:** Los 4 escenarios pasan en `mvn test`.
  - **Archivos:** `src/test/java/edward/com/finanzasbackend/auth/LoginAndTokenIT.java`

- [x] **T-24 — Test de integración end-to-end: flujo de recuperación de contraseña**
  - Escenario 1: solicitud de reset con email existente → `200` (sin revelar si existe).
  - Escenario 2: solicitud de reset con email inexistente → `200` (mismo response).
  - Escenario 3: reset con token válido → contraseña actualizada → login con nueva contraseña → `200`.
  - Escenario 4: reset con token ya usado → `400`.
  - **Hecho cuando:** Los 4 escenarios pasan en `mvn test`.
  - **Archivos:** `src/test/java/edward/com/finanzasbackend/auth/PasswordResetIT.java`

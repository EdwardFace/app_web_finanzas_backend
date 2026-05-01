# Tasks: Account Module

**Feature:** account-module  
**Fecha:** 2026-04-30  
**Spec:** requirements.md · design.md

---

## Limpieza previa

- [ ] **T-01 — Eliminar archivo mal ubicado**  
  Borrar `src/main/java/edward/com/finanzasbackend/account/domain/exception/Account.java` (entidad incompleta en paquete equivocado).  
  **Hecho cuando:** el archivo no existe en el filesystem.  
  **Archivos:** `account/domain/exception/Account.java` ← eliminar

---

## Capa de dominio

- [ ] **T-02 — Crear enum `AccountType`**  
  Enum con valores `CASH`, `BANK`, `CREDIT`.  
  **Hecho cuando:** el enum compila y los 3 valores están definidos.  
  **Archivos:** `account/domain/AccountType.java` ← crear

- [ ] **T-03 — Crear entidad JPA `Account`**  
  Entidad con campos: `id` (Long, PK), `name`, `type` (AccountType), `balance` (BigDecimal), `userId` (UUID), `createdAt`, `updatedAt`, `deletedAt`. `@PrePersist` inicializa `balance = BigDecimal.ZERO`, `createdAt` y `updatedAt`. `@PreUpdate` actualiza `updatedAt`. Tabla `accounts`.  
  **Hecho cuando:** la clase compila con todas las anotaciones JPA y getters/setters.  
  **Archivos:** `account/domain/Account.java` ← crear  
  **Depende de:** T-01, T-02

- [ ] **T-04 — Crear excepción `AccountNotFoundException`**  
  Extiende `RuntimeException`. Constructor recibe `Long id` y genera mensaje `"Account not found: {id}"`.  
  **Hecho cuando:** la clase compila y el mensaje es correcto.  
  **Archivos:** `account/domain/exception/AccountNotFoundException.java` ← crear

- [ ] **T-05 — Crear excepción `AccountAccessDeniedException`**  
  Extiende `RuntimeException`. Constructor sin parámetros con mensaje `"Access denied to this account"`.  
  **Hecho cuando:** la clase compila.  
  **Archivos:** `account/domain/exception/AccountAccessDeniedException.java` ← crear

---

## Capa de infraestructura

- [ ] **T-06 — Crear `AccountRepository`**  
  Interface que extiende `JpaRepository<Account, Long>` con dos métodos derivados:  
  - `List<Account> findByUserIdAndDeletedAtIsNull(UUID userId)`  
  - `Optional<Account> findByIdAndDeletedAtIsNull(Long id)`  
  **Hecho cuando:** la interface compila y Spring Data puede resolver ambos métodos sin query manual.  
  **Archivos:** `account/infrastructure/AccountRepository.java` ← crear  
  **Depende de:** T-03

---

## DTOs

- [ ] **T-07 — Crear DTOs de request y response**  
  Tres Java records en `account/api/dto/`:  
  - `CreateAccountRequest`: `@NotBlank String name`, `@NotNull AccountType type`  
  - `UpdateAccountRequest`: `String name` (nullable), `AccountType type` (nullable)  
  - `AccountResponse`: `Long id`, `String name`, `AccountType type`, `BigDecimal balance`, `LocalDateTime createdAt`, `LocalDateTime updatedAt`  
  **Hecho cuando:** los 3 records compilan con las anotaciones de validación correctas.  
  **Archivos:** `account/api/dto/CreateAccountRequest.java`, `UpdateAccountRequest.java`, `AccountResponse.java` ← crear  
  **Depende de:** T-02

---

## Capa de aplicación

- [ ] **T-08 — Crear `AccountService`**  
  `@Service` con los 5 métodos públicos. Cada método extrae la lógica descrita en el diseño:  
  - `create(CreateAccountRequest, UUID userId) → AccountResponse`  
  - `findAll(UUID userId) → List<AccountResponse>`  
  - `findById(Long id, UUID userId) → AccountResponse` — lanza `AccountNotFoundException` / `AccountAccessDeniedException`  
  - `update(Long id, UpdateAccountRequest, UUID userId) → AccountResponse` — aplica solo campos no-null  
  - `delete(Long id, UUID userId)` — soft delete vía `setDeletedAt(LocalDateTime.now())`  
  **Hecho cuando:** todos los métodos implementados, la lógica de ownership está en cada operación que lo requiere, y el servicio compila sin errores.  
  **Archivos:** `account/application/AccountService.java` ← crear  
  **Depende de:** T-03, T-04, T-05, T-06, T-07

---

## Capa de API

- [ ] **T-09 — Crear `AccountController`**  
  `@RestController @RequestMapping("/accounts")` con 5 endpoints. El userId se extrae del `SecurityContextHolder` (`UUID.fromString(authentication.getName())`), nunca del body. Métodos y status codes:  
  - `POST /accounts` → 201  
  - `GET /accounts` → 200  
  - `GET /accounts/{id}` → 200  
  - `PUT /accounts/{id}` → 200  
  - `DELETE /accounts/{id}` → 204  
  **Hecho cuando:** el controlador compila, todos los endpoints están mapeados y usan `@Valid` donde corresponde.  
  **Archivos:** `account/api/AccountController.java` ← crear  
  **Depende de:** T-07, T-08

---

## Modificaciones a módulo auth

- [ ] **T-10 — Agregar handlers al `GlobalExceptionHandler`**  
  Agregar dos métodos `@ExceptionHandler` al handler existente:  
  - `AccountNotFoundException` → 404  
  - `AccountAccessDeniedException` → 403  
  El formato de respuesta `Map.of("error", message)` ya existe, reutilizarlo.  
  **Hecho cuando:** los dos handlers están presentes y el handler compila.  
  **Archivos:** `auth/api/GlobalExceptionHandler.java` ← modificar  
  **Depende de:** T-04, T-05

- [ ] **T-11 — Proteger rutas `/accounts/**` en `SecurityConfig`**  
  Agregar `.requestMatchers("/accounts/**").authenticated()` antes de `.anyRequest().permitAll()` en la cadena de `authorizeHttpRequests`.  
  **Hecho cuando:** la config compila y un request sin JWT a `/accounts` devuelve 401 (verificable arrancando la app o con test).  
  **Archivos:** `auth/security/SecurityConfig.java` ← modificar

---

## Tests unitarios

- [ ] **T-12 — Tests unitarios de `AccountService`**  
  Clase `AccountServiceTest` con `@ExtendWith(MockitoExtension.class)`. Cubrir por cada método:  
  - `create`: happy path → cuenta guardada con balance 0  
  - `findAll`: retorna solo cuentas activas del usuario  
  - `findById`: happy path · cuenta no encontrada → `AccountNotFoundException` · cuenta de otro usuario → `AccountAccessDeniedException`  
  - `update`: happy path · not found · access denied · solo actualiza campos no-null  
  - `delete`: happy path (soft delete seteado) · not found · access denied  
  **Hecho cuando:** todos los casos pasan con `mvn test`.  
  **Archivos:** `src/test/.../account/application/AccountServiceTest.java` ← crear  
  **Depende de:** T-08

- [ ] **T-13 — Tests unitarios de `AccountController`**  
  Clase `AccountControllerTest` con MockMvc (`@WebMvcTest`). Cubrir por endpoint:  
  - `POST /accounts`: 201 con body válido · 400 con body inválido  
  - `GET /accounts`: 200 con lista  
  - `GET /accounts/{id}`: 200 · 403 · 404  
  - `PUT /accounts/{id}`: 200 · 403 · 404  
  - `DELETE /accounts/{id}`: 204 · 403 · 404  
  **Hecho cuando:** todos los casos pasan con `mvn test`.  
  **Archivos:** `src/test/.../account/api/AccountControllerTest.java` ← crear  
  **Depende de:** T-09

---

## Tests de integración

- [ ] **T-14 — Test de integración `AccountCrudIT`**  
  Clase con `@SpringBootTest(webEnvironment = RANDOM_PORT)` y `@ActiveProfiles("test")`. Flujo completo autenticado:  
  1. Registrar y verificar un usuario (reutilizar helpers de los IT de auth)  
  2. Login → obtener JWT  
  3. `POST /accounts` → verificar 201 y balance = 0  
  4. `GET /accounts` → verificar que aparece la cuenta creada  
  5. `PUT /accounts/{id}` → verificar que el nombre se actualizó  
  6. `DELETE /accounts/{id}` → verificar 204  
  7. `GET /accounts/{id}` → verificar 404 tras el soft delete  
  8. Verificar 403 al intentar acceder a la cuenta con otro usuario  
  **Hecho cuando:** el test completo pasa con `mvn test` contra la base de datos de test.  
  **Archivos:** `src/test/.../account/AccountCrudIT.java` ← crear  
  **Depende de:** T-09, T-10, T-11

---

## Orden de implementación sugerido

```
T-01 → T-02 → T-03 → T-04, T-05 (paralelo)
             ↓
            T-06 → T-07 (paralelo con T-06)
                     ↓
                    T-08
                     ↓
                    T-09 → T-10, T-11 (paralelo)
                              ↓
                         T-12, T-13 (paralelo)
                              ↓
                             T-14
```

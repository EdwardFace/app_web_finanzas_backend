# Design: Account Module

**Fecha:** 2026-04-30  
**Estado:** Pendiente de aprobación

---

## 1. Arquitectura propuesta

El módulo sigue exactamente la misma estructura hexagonal del módulo `auth` ya implementado:

```
edward.com.finanzasbackend.account/
├── domain/
│   ├── Account.java                          ← Entidad JPA
│   ├── AccountType.java                      ← Enum (CASH, BANK, CREDIT)
│   └── exception/
│       ├── AccountNotFoundException.java
│       └── AccountAccessDeniedException.java
├── application/
│   └── AccountService.java                   ← Lógica de negocio
├── infrastructure/
│   └── AccountRepository.java                ← JpaRepository
└── api/
    ├── AccountController.java                ← REST controller
    └── dto/
        ├── CreateAccountRequest.java
        ├── UpdateAccountRequest.java
        └── AccountResponse.java
```

---

## 2. Componentes a crear o modificar

### CREAR (nuevos)

| Archivo | Tipo | Descripción |
|---------|------|-------------|
| `account/domain/Account.java` | Entity JPA | Entidad principal con soft delete |
| `account/domain/AccountType.java` | Enum | CASH, BANK, CREDIT |
| `account/domain/exception/AccountNotFoundException.java` | Exception | 404 - cuenta no encontrada o eliminada |
| `account/domain/exception/AccountAccessDeniedException.java` | Exception | 403 - cuenta de otro usuario |
| `account/application/AccountService.java` | Service | Operaciones CRUD con validación de ownership |
| `account/infrastructure/AccountRepository.java` | Repository | Queries con filtro por userId y deletedAt |
| `account/api/AccountController.java` | Controller | 5 endpoints REST |
| `account/api/dto/CreateAccountRequest.java` | DTO (record) | name + type |
| `account/api/dto/UpdateAccountRequest.java` | DTO (record) | name + type (ambos opcionales) |
| `account/api/dto/AccountResponse.java` | DTO (record) | Respuesta pública de la cuenta |

### MODIFICAR (existentes)

| Archivo | Cambio |
|---------|--------|
| `auth/api/GlobalExceptionHandler.java` | Agregar handlers para `AccountNotFoundException` (404) y `AccountAccessDeniedException` (403) |
| `auth/security/SecurityConfig.java` | Agregar `.requestMatchers("/accounts/**").authenticated()` antes de `.anyRequest().permitAll()` |

### ELIMINAR

| Archivo | Motivo |
|---------|--------|
| `account/domain/exception/Account.java` | Archivo mal ubicado e incompleto — se reemplaza con la entidad correcta en `account/domain/Account.java` |

---

## 3. Interfaces y contratos de datos

### 3.1 Entidad JPA — `Account`

```java
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "user_id", nullable = false)
    private UUID userId;                      // UUID — coincide con User.id

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;          // null = activa | not null = eliminada

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        balance   = BigDecimal.ZERO;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

> **Nota sobre userId:** La entidad `User` usa `UUID` como PK (`@GeneratedValue(strategy = GenerationType.UUID)`). Por consistencia, `Account.userId` será `UUID`, no `Long` (el CLAUDE.md usa Long de forma genérica, pero la implementación real dicta UUID).

### 3.2 DTOs (Java records, igual que el módulo auth)

**`CreateAccountRequest`**
```java
public record CreateAccountRequest(
    @NotBlank String name,
    @NotNull AccountType type
) {}
```

**`UpdateAccountRequest`**
```java
public record UpdateAccountRequest(
    String name,     // opcional — null = no cambiar
    AccountType type // opcional — null = no cambiar
) {}
```

**`AccountResponse`**
```java
public record AccountResponse(
    Long id,
    String name,
    AccountType type,
    BigDecimal balance,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### 3.3 Repository — `AccountRepository`

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Listado activo del usuario
    List<Account> findByUserIdAndDeletedAtIsNull(UUID userId);

    // Búsqueda por id y activa
    Optional<Account> findByIdAndDeletedAtIsNull(Long id);
}
```

### 3.4 Endpoints REST — `AccountController`

| Método | Path           | Request Body            | Response              | Status codes       |
|--------|----------------|-------------------------|-----------------------|--------------------|
| POST   | /accounts      | `CreateAccountRequest`  | `AccountResponse`     | 201, 400, 401      |
| GET    | /accounts      | —                       | `List<AccountResponse>` | 200, 401         |
| GET    | /accounts/{id} | —                       | `AccountResponse`     | 200, 401, 403, 404 |
| PUT    | /accounts/{id} | `UpdateAccountRequest`  | `AccountResponse`     | 200, 400, 401, 403, 404 |
| DELETE | /accounts/{id} | —                       | (vacío)               | 204, 401, 403, 404 |

### 3.5 Extracción del userId desde el JWT

El `JwtAuthFilter` ya registra el `sub` del JWT (UUID del usuario) como `principal` en el `SecurityContextHolder`:

```java
// En AccountController — patrón a usar
private UUID currentUserId() {
    String subject = SecurityContextHolder.getContext()
                         .getAuthentication().getName();
    return UUID.fromString(subject);
}
```

No se necesita modificar la seguridad existente para obtener el userId.

### 3.6 Manejo de excepciones — nuevos handlers en `GlobalExceptionHandler`

```java
@ExceptionHandler(AccountNotFoundException.class)
ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
}

@ExceptionHandler(AccountAccessDeniedException.class)
ResponseEntity<Map<String, String>> handleAccountAccessDenied(AccountAccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(ex.getMessage()));
}
```

---

## 4. Flujos clave

### Crear cuenta (POST /accounts)
```
Controller → extrae userId del JWT
          → valida request (@Valid)
          → AccountService.create(request, userId)
             → new Account(name, type, userId)
             → balance = 0.00 por @PrePersist
             → accountRepository.save(account)
          → retorna AccountResponse — 201 Created
```

### Eliminar cuenta (DELETE /accounts/{id}) — soft delete
```
Controller → extrae userId del JWT
          → AccountService.delete(id, userId)
             → findByIdAndDeletedAtIsNull(id) → si no existe → AccountNotFoundException
             → account.getUserId().equals(userId) → si no → AccountAccessDeniedException
             → account.setDeletedAt(LocalDateTime.now())
             → accountRepository.save(account)
          → retorna 204 No Content
```

### Actualizar cuenta (PUT /accounts/{id})
```
Controller → extrae userId del JWT
          → AccountService.update(id, request, userId)
             → findByIdAndDeletedAtIsNull(id) → si no existe → AccountNotFoundException
             → ownership check → si falla → AccountAccessDeniedException
             → aplica campos no-null del request
             → accountRepository.save(account) → @PreUpdate setea updatedAt
          → retorna AccountResponse — 200 OK
```

---

## 5. Tests a implementar

Siguiendo los patrones del módulo `auth`:

### Unit tests (con Mockito)
- `AccountServiceTest` — cubre los 5 métodos del servicio con casos: happy path, not found, access denied
- `AccountControllerTest` — cubre los 5 endpoints con MockMvc: happy path, 401, 403, 404, 400

### Integration tests (con @SpringBootTest)
- `AccountCrudIT` — flujo completo: crear → listar → actualizar → eliminar

---

## 6. Migración de base de datos

Nueva tabla `accounts`:

```sql
CREATE TABLE accounts (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    balance    NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    user_id    UUID         NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    deleted_at TIMESTAMP
);
```

> El proyecto usa JPA con `spring.jpa.hibernate.ddl-auto`. Si está en `update` o `create`, Hibernate crea la tabla automáticamente. Si hay Flyway/Liquibase, habrá que agregar un script de migración.

---

## 7. Dependencias y riesgos

### Dependencias
| Módulo | Relación | Detalle |
|--------|----------|---------|
| `auth` | Requerida | `GlobalExceptionHandler` y `SecurityConfig` se modifican |
| `auth` | Requerida | `JwtAuthFilter` provee el userId via `SecurityContextHolder` |
| `transaction` | Ninguna (por ahora) | Balance no se actualiza en este scope |

### Riesgos

| Riesgo | Probabilidad | Mitigación |
|--------|-------------|------------|
| `SecurityConfig` está en `auth` — modificarlo puede romper tests del módulo auth | Media | Agregar el matcher de forma aditiva; ejecutar tests de auth después del cambio |
| `Account.java` existe en `account/domain/exception/` — conflicto de paquete | Alta | Eliminar el archivo antes de crear el nuevo en `account/domain/` |
| `User.id` es UUID pero `CLAUDE.md` dice Long — inconsistencia de tipos | Baja | Confirmado en código: usar UUID en `Account.userId` |
| Múltiples módulos futuros también necesitarán modificar `GlobalExceptionHandler` | Alta (a futuro) | Aceptable por ahora; considerar mover a `shared` cuando haya ≥ 3 módulos |

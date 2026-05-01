# Requirements: Account Module

**Fecha:** 2026-04-30  
**Estado:** Pendiente de aprobación

---

## Objetivo

Implementar el módulo de gestión de cuentas financieras (`account`) dentro de la arquitectura monolítica modular + hexagonal del proyecto. El módulo debe permitir a cada usuario autenticado crear y administrar sus propias cuentas (efectivo, banco, crédito), con aislamiento total entre usuarios.

---

## Historias de usuario

### HU-01 — Crear cuenta
> Como usuario autenticado, quiero crear una cuenta financiera con nombre y tipo, para registrar mis distintos medios de pago o ahorro.

### HU-02 — Listar cuentas
> Como usuario autenticado, quiero ver todas mis cuentas financieras, para tener una visión general de mis activos y pasivos.

### HU-03 — Ver detalle de cuenta
> Como usuario autenticado, quiero consultar el detalle de una cuenta específica, para revisar su nombre, tipo y balance actual.

### HU-04 — Actualizar cuenta
> Como usuario autenticado, quiero modificar el nombre o tipo de una cuenta, para mantener mi información financiera actualizada.

### HU-05 — Eliminar cuenta
> Como usuario autenticado, quiero eliminar una cuenta que ya no uso, para mantener limpia mi lista de cuentas.

---

## Criterios de aceptación

### HU-01 — Crear cuenta
- [ ] `POST /accounts` crea una cuenta vinculada al `userId` extraído del JWT
- [ ] El campo `name` es obligatorio y no puede estar vacío
- [ ] El campo `type` es obligatorio y solo acepta los valores `CASH`, `BANK`, `CREDIT`
- [ ] El `balance` se inicializa automáticamente en `0.00` — no es enviado por el cliente
- [ ] Retorna `201 Created` con el recurso creado
- [ ] No es posible crear una cuenta en nombre de otro usuario

### HU-02 — Listar cuentas
- [ ] `GET /accounts` retorna todas las cuentas del usuario autenticado
- [ ] Un usuario no puede ver cuentas de otro usuario
- [ ] Si el usuario no tiene cuentas, retorna `200 OK` con lista vacía `[]`
- [ ] Las cuentas con soft delete (`deletedAt != null`) no aparecen en el listado

### HU-03 — Ver detalle de cuenta
- [ ] `GET /accounts/{id}` retorna el detalle de una cuenta
- [ ] Si la cuenta no pertenece al usuario autenticado, retorna `403 Forbidden`
- [ ] Si la cuenta no existe o fue eliminada, retorna `404 Not Found`

### HU-04 — Actualizar cuenta
- [ ] `PUT /accounts/{id}` permite modificar `name` y/o `type`
- [ ] No se puede modificar el `balance` directamente desde este endpoint
- [ ] Si la cuenta no pertenece al usuario autenticado, retorna `403 Forbidden`
- [ ] Si la cuenta no existe o fue eliminada, retorna `404 Not Found`
- [ ] Retorna `200 OK` con el recurso actualizado

### HU-05 — Eliminar cuenta
- [ ] `DELETE /accounts/{id}` realiza un **soft delete** (setea `deletedAt` con la fecha actual)
- [ ] Si la cuenta no pertenece al usuario autenticado, retorna `403 Forbidden`
- [ ] Si la cuenta no existe o ya fue eliminada, retorna `404 Not Found`
- [ ] Retorna `204 No Content` al eliminar exitosamente
- [ ] Una cuenta con soft delete no es recuperable desde la API (en este scope)

### Seguridad transversal
- [ ] Todos los endpoints requieren JWT válido (`Authorization: Bearer <token>`)
- [ ] El `userId` siempre se extrae del token, nunca del body del request
- [ ] Sin JWT válido, retorna `401 Unauthorized`

---

## Modelo de datos

```
Account
├── id          (Long, PK, auto-generated)
├── name        (String, not null)
├── type        (Enum: CASH | BANK | CREDIT, not null)
├── balance     (BigDecimal, default 0.00, not null)
├── userId      (Long, not null) — referencia al usuario dueño
├── createdAt   (Date, auto)
├── updatedAt   (Date, auto)
└── deletedAt   (Date, nullable) — null = activa, not null = eliminada
```

---

## Endpoints esperados

| Método | Ruta            | Descripción             | Auth |
|--------|-----------------|-------------------------|------|
| POST   | /accounts       | Crear cuenta            | JWT  |
| GET    | /accounts       | Listar cuentas propias  | JWT  |
| GET    | /accounts/{id}  | Ver detalle de cuenta   | JWT  |
| PUT    | /accounts/{id}  | Actualizar cuenta       | JWT  |
| DELETE | /accounts/{id}  | Eliminar cuenta (soft)  | JWT  |

---

## Fuera de scope (esta iteración)

- **Currency / multi-moneda:** el campo `currency` no se incluye en esta versión
- **Actualización manual de balance:** el balance no es editable directamente; se calculará al implementar el módulo de transacciones
- **Relación JPA con Transaction:** la entidad `Account` no tendrá colección de transacciones mapeada por ahora
- **Filtros y paginación** en el listado de cuentas
- **Recuperación de cuentas eliminadas** (undelete)
- **Roles de administrador:** no existe visibilidad cruzada entre usuarios
- **Transferencias entre cuentas**

---

## Dependencias

- Módulo `auth` ya implementado: se reutiliza el `User` y la extracción de `userId` desde el JWT (`JwtAuthFilter`)
- No hay dependencia con el módulo `transaction` en esta iteración

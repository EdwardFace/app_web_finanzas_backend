# Requisitos: User Authentication

## Objetivo
Permitir que los usuarios accedan de forma segura al sistema de finanzas personales mediante registro propio con verificación de email, login con credenciales o Google OAuth2, y gestión de sesión con JWT (access + refresh token).

---

## Historias de usuario

### Registro
- **US-01** — Como usuario nuevo, quiero registrarme con mi email y contraseña, para crear una cuenta en el sistema.
- **US-02** — Como usuario nuevo, quiero recibir un email de verificación tras registrarme, para confirmar que mi dirección de correo es válida y activar mi cuenta.
- **US-03** — Como usuario con cuenta no verificada, quiero poder reenviar el email de verificación, para poder activar mi cuenta si el correo original no llegó.

### Login
- **US-04** — Como usuario registrado y verificado, quiero iniciar sesión con mi email y contraseña, para acceder a mis finanzas.
- **US-05** — Como usuario, quiero iniciar sesión con mi cuenta de Google, para acceder sin necesidad de crear credenciales propias.
- **US-06** — Como usuario autenticado, quiero recibir un access token y un refresh token al hacer login, para mantener mi sesión activa de forma segura.

### Renovación de sesión
- **US-07** — Como usuario autenticado, quiero renovar mi access token usando el refresh token, para no tener que volver a iniciar sesión cuando el access token expire.

### Logout
- **US-08** — Como usuario autenticado, quiero cerrar sesión, para que mis tokens queden invalidados y nadie más pueda usarlos.

### Recuperación de contraseña
- **US-09** — Como usuario registrado, quiero solicitar un enlace de recuperación de contraseña a mi email, para recuperar el acceso si olvidé mis credenciales.
- **US-10** — Como usuario que recibió el enlace de recuperación, quiero establecer una nueva contraseña, para volver a acceder a mi cuenta.

---

## Criterios de aceptación

### US-01 — Registro con email/password
- [ ] El endpoint `POST /api/auth/register` acepta `email`, `password` y `name`.
- [ ] El email debe ser único; si ya existe, retorna `409 Conflict`.
- [ ] La contraseña se almacena hasheada con BCrypt (nunca en texto plano).
- [ ] El usuario queda en estado `PENDING_VERIFICATION` hasta confirmar el email.
- [ ] Retorna `201 Created` con el ID del usuario recién creado.

### US-02 — Verificación de email
- [ ] Al registrarse, el sistema envía un email con un token de verificación de un solo uso.
- [ ] El token expira en 24 horas.
- [ ] El endpoint `GET /api/auth/verify-email?token={token}` activa la cuenta (`ACTIVE`).
- [ ] Un token ya usado o expirado retorna `400 Bad Request` con mensaje descriptivo.

### US-03 — Reenvío de email de verificación
- [ ] El endpoint `POST /api/auth/resend-verification` acepta el email del usuario.
- [ ] Solo reenvía si la cuenta existe y está en estado `PENDING_VERIFICATION`.
- [ ] Invalida el token anterior y genera uno nuevo.

### US-04 — Login con email/password
- [ ] El endpoint `POST /api/auth/login` acepta `email` y `password`.
- [ ] Si las credenciales son incorrectas, retorna `401 Unauthorized`.
- [ ] Si la cuenta no está verificada, retorna `403 Forbidden` con mensaje claro.
- [ ] En caso exitoso, retorna `access_token` (15 min de vida) y `refresh_token` (7 días de vida).

### US-05 — Login con Google OAuth2
- [ ] El flujo inicia en `GET /api/auth/oauth2/google`.
- [ ] Si el email de Google no existe en el sistema, se crea la cuenta automáticamente en estado `ACTIVE` (sin necesidad de verificación de email).
- [ ] Si el email ya existe con registro local, se vincula la cuenta de Google al usuario existente.
- [ ] Al completar el flujo OAuth2, retorna los mismos `access_token` y `refresh_token` que el login local.

### US-06 / US-07 — JWT y renovación de sesión
- [ ] El access token es un JWT firmado con RS256 o HS256 con expiración de 15 minutos.
- [ ] El refresh token se almacena en base de datos (para poder invalidarlo).
- [ ] El endpoint `POST /api/auth/refresh` acepta el `refresh_token` y retorna un nuevo `access_token`.
- [ ] Si el refresh token es inválido, expirado o fue revocado, retorna `401 Unauthorized`.

### US-08 — Logout
- [ ] El endpoint `POST /api/auth/logout` requiere el `refresh_token` en el body.
- [ ] El sistema marca el refresh token como revocado en base de datos.
- [ ] Retorna `204 No Content`.

### US-09 — Solicitar recuperación de contraseña
- [ ] El endpoint `POST /api/auth/forgot-password` acepta un email.
- [ ] Si el email no existe, retorna igualmente `200 OK` (para no revelar si el email está registrado).
- [ ] Envía un email con un enlace que contiene un token de un solo uso, válido por 1 hora.

### US-10 — Restablecer contraseña
- [ ] El endpoint `POST /api/auth/reset-password` acepta `token` y `newPassword`.
- [ ] Si el token es válido, actualiza la contraseña (hasheada con BCrypt) e invalida el token.
- [ ] Si el token es inválido o expirado, retorna `400 Bad Request`.
- [ ] Revoca todos los refresh tokens activos del usuario al cambiar la contraseña.

---

## Fuera de scope

- Autenticación multifactor (2FA / MFA)
- Bloqueo de cuenta por intentos fallidos
- Login con otros proveedores OAuth (Facebook, GitHub, etc.)
- Roles y permisos (RBAC)
- Administración de usuarios por parte de un admin
- Auditoría de accesos / historial de sesiones

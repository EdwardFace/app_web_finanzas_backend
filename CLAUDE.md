# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Mi proyecto

Este documento define las entidades principales del sistema, organizadas por módulo dentro de una arquitectura **monolítica modular + hexagonal**.

# 🔐 Módulo: auth

## 📌 Responsabilidad

Gestión de autenticación y autorización.

## 🧩 Entidades

### AuthToken (opcional)

* id (Long)
* userId (Long)
* accessToken (String)
* refreshToken (String)
* expirationDate (Date)

---

# Módulo: user

## Responsabilidad

Gestión de usuarios y perfil.

## Entidades

### User

* id (Long)
* email (String, unique)
* password (String)
* createdAt (Date)
* updatedAt (Date)

---

# Módulo: account

## Responsabilidad

Gestión de cuentas financieras.

## Entidades

### Account

* id (Long)
* name (String)
* type (Enum: CASH, BANK, CREDIT)
* balance (BigDecimal)
* currency (String) *(opcional)*
* userId (Long)
* createdAt (Date)
* updatedAt (Date)

---

# 💸 Módulo: transaction

## 📌 Responsabilidad

Gestión de movimientos financieros.

## 🧩 Entidades

### Transaction

* id (Long)
* amount (BigDecimal)
* type (Enum: INCOME, EXPENSE)
* description (String)
* date (Date)
* accountId (Long)
* categoryId (Long)
* userId (Long)
* createdAt (Date)

---

### TransactionEvent (dominio)

* id (Long)
* type (String)
* payload (JSON)
* createdAt (Date)

---

# 🏷️ Módulo: category

## 📌 Responsabilidad

Clasificación de transacciones.

## 🧩 Entidades

### Category

* id (Long)
* name (String)
* type (Enum: INCOME, EXPENSE)
* userId (Long)
* createdAt (Date)

---

# 🎯 Módulo: budget

## 📌 Responsabilidad

Control de presupuestos por categoría.

## 🧩 Entidades

### Budget

* id (Long)
* limitAmount (BigDecimal)
* month (Integer)
* year (Integer)
* categoryId (Long)
* userId (Long)
* createdAt (Date)

---

# 📊 Módulo: report

## 📌 Responsabilidad

Consultas agregadas (solo lectura).

## 🧩 Entidades (DTO / Proyecciones)

### MonthlySummary

* totalIncome (BigDecimal)
* totalExpense (BigDecimal)
* balance (BigDecimal)

---

### CategoryReport

* categoryName (String)
* totalAmount (BigDecimal)

---

# 🔔 Módulo: notification

## 📌 Responsabilidad

Gestión de notificaciones del sistema.

## 🧩 Entidades

### Notification

* id (Long)
* message (String)
* type (String)
* userId (Long)
* read (Boolean)
* createdAt (Date)

---

# 📥 Módulo: importdata

## 📌 Responsabilidad

Importación de datos externos.

## 🧩 Entidades

### ImportJob

* id (Long)
* fileName (String)
* status (Enum: PENDING, PROCESSING, COMPLETED, FAILED)
* processedRecords (Integer)
* createdAt (Date)

---

# ⚙️ Módulo: shared

## 📌 Responsabilidad

Componentes transversales.

## 🧩 Entidades base

### BaseEntity (recomendado)

* id (Long)
* createdAt (Date)
* updatedAt (Date)

---

# 🔗 Relaciones clave

* User 1:N Account
* User 1:N Transaction
* User 1:N Category
* User 1:N Budget
* Account 1:N Transaction
* Category 1:N Transaction
* Category 1:N Budget

---

# ⚠️ Decisiones de diseño

* Todas las entidades principales incluyen `userId` para aislamiento multi-usuario.
* Se privilegia desacoplamiento entre módulos (no relaciones JPA bidireccionales innecesarias).
* Report usa DTOs, no entidades persistentes.
* Eventos de dominio permiten comunicación entre módulos.

---

# 🚀 Extensiones futuras

* Multi-moneda (currency + exchangeRate)
* Transacciones recurrentes
* Soft delete (deletedAt)
* Auditoría avanzada

---

# 🧱 Resumen

Este modelo:

* Soporta arquitectura modular
* Permite escalar sin romper diseño
* Es apto para aplicar Clean Architecture
* Está optimizado para backend profesional con Spring Boot

---


## Stack técnico
- Framework: Springboot 4.x.x
- Lenguaje: Java 21
- Tests: Junit5 + mockito
- Persistencia: Hibernate + JPA
- Base de datos: Postgresql
- Autenticacion y Autorizacion: Spring Security

## Reglas de desarrollo
- Siempre escribe tests para nueva funcionalidad 
- Sigue el patrón de módulos en `/src/features/`
- Usa commits semánticos (feat:, fix:, refactor:)

## Comandos clave
- `mvn spring-boot:run` — servidor de desarrollo
- `mvn spring-boot:test-run` — suite de tests
- `mvn clean package` — build de producción


# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Mi proyecto
Este proyecto consta de un sistema web para administrar finanzas personales, donde los usuarios pueden:
- Crear transacción 
- Calcular balance
- Generar reportes

El proyecto esta basado en una arquitectura monolitica modular

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

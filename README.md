# SIRA · Sistema de Rutinas y Apoyos

[![CI](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml/badge.svg)](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml)

Aplicación web para un centro de terapia ocupacional que atiende a personas autistas y a sus familias. El profesional diseña rutinas por pasos y se las asigna a un participante; en la casa, el encargado registra cada día cómo salió cada paso; el sistema calcula la adherencia en el tiempo.

Proyecto del curso EIF509 Desarrollo de Aplicaciones Basadas en Web, NRC 51092, Grupo G01, II Ciclo 2026. Universidad Nacional. Anthony Cerdas Chacón, trabajo individual.

## Documentación

- [Propuesta de dominio](docs/propuesta-dominio.md)
- [Arquitectura](docs/arquitectura.md)
- [ADR-001: organización en capas](docs/adr/ADR-001-arquitectura-en-capas.md)

## Stack

Java 21, Spring Boot 3.3.13, Gradle con wrapper, JUnit 5 y AssertJ. PostgreSQL y MongoDB entran en el Lab 2; el frontend React en el Lab 6.

## Cómo correrlo

Solo hace falta Java 21 (`java -version` tiene que decir 21.x). El resto lo descarga el wrapper.

```bash
./gradlew build
```

Compila y corre las pruebas. En Windows es `gradlew.bat build`.

```bash
./gradlew bootRun
```

Levanta la aplicación en el puerto 8080. Con la app corriendo:

| Endpoint | Respuesta |
|----------|-----------|
| `GET /api/salud` | `{"estado":"OK - SIRA en linea"}` |
| `GET /api/participantes` | `[{"id":1,"nombre":"Participante A"},{"id":2,"nombre":"Participante B"}]` |
| `GET /actuator/health` | `{"status":"UP"}` |

Los participantes son una lista en memoria, porque todavía no hay base de datos. Devuelve 2 de los 3: el tercero está inactivo y la regla de negocio lo excluye.

## Estructura

```
src/main/java/cr/ac/una/sira/
├── presentation/   Controladores y DTOs
├── business/       Servicios, reglas de negocio
├── data/           Repositorios y modelo
└── config/         Configuración de Spring
```

Las dependencias van en una sola dirección: `presentación -> negocio -> datos`.

## Pruebas

```bash
./gradlew test
```

`SiraApplicationTests` verifica que el contexto de Spring levanta. `ParticipanteServiceTest` verifica que el listado excluye inactivos y ordena por nombre, sin levantar Spring.

## Integración continua

Cada push a `main` dispara [`.github/workflows/ci.yml`](.github/workflows/ci.yml), que compila y corre las pruebas en una máquina Linux limpia con Java 21. El estado se ve en el badge de arriba.

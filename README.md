# SIRA · Sistema de Rutinas y Apoyos

[![CI](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml/badge.svg)](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml)

Aplicación web para un centro de terapia ocupacional que atiende a personas autistas y a sus familias. El profesional diseña rutinas por pasos y se las asigna a un participante; en la casa, el encargado registra cada día cómo salió cada paso; el sistema calcula la adherencia en el tiempo.

Proyecto del curso EIF509 Desarrollo de Aplicaciones Basadas en Web, NRC 51092, Grupo G01, II Ciclo 2026. Universidad Nacional. Anthony Cerdas Chacón, trabajo individual.

## Documentación

- [Propuesta de dominio](docs/propuesta-dominio.md)
- [Arquitectura](docs/arquitectura.md)
- [Laboratorio 2: capa de datos completa](docs/lab2-capa-datos.md)
- [Laboratorio 3: ORM, repositorios, consultas y N+1](docs/lab3-persistencia-orm.md)
- [ADR-001: organización en capas](docs/adr/ADR-001-arquitectura-en-capas.md)

## Stack

Java 21, Spring Boot 3.3.13, Spring Data JPA/MongoDB, Hibernate, Flyway,
PostgreSQL 16, MongoDB 7, Testcontainers, Gradle, JUnit 5 y AssertJ.

## Cómo correrlo

Solo hace falta Java 21 (`java -version` tiene que decir 21.x). El resto lo descarga el wrapper.

```bash
./gradlew build
```

Compila y corre las pruebas. En Windows es `gradlew.bat build`.

Para levantar la capa de datos del Laboratorio 2:

```bash
docker compose up -d
```

Esto inicia PostgreSQL, aplica las migraciones Flyway ubicadas en `src/main/resources/db/migration` y carga la coleccion documental de MongoDB con datos de ejemplo. Para reconstruir todo desde cero:

```bash
docker compose down -v
docker compose up -d
```

```bash
./gradlew bootRun
```

Levanta la aplicación en el puerto 8080. Con la app corriendo:

| Endpoint | Respuesta |
|----------|-----------|
| `GET /api/salud` | `{"estado":"OK - SIRA en linea"}` |
| `GET /api/participantes` | `[{"id":1,"nombre":"Mateo Mendez"},{"id":2,"nombre":"Sofia Solano"}]` |
| `GET /actuator/health` | `{"status":"UP"}` |

Los endpoints consultan PostgreSQL mediante Spring Data JPA. Hibernate valida el
esquema creado por Flyway y la bitacora documental se accede mediante el
repositorio de Spring Data MongoDB.

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

Las pruebas unitarias validan la configuracion y el servicio sin levantar Spring.
Ocho pruebas de integracion usan Testcontainers con PostgreSQL 16 real para
validar migraciones, mapeos `LAZY`, CRUD generico, JPQL, Criteria y la correccion
del problema N+1.

## Integración continua

Cada push a `main` dispara [`.github/workflows/ci.yml`](.github/workflows/ci.yml), que compila y corre las pruebas en una máquina Linux limpia con Java 21. El estado se ve en el badge de arriba.

# SIRA · Sistema de Rutinas y Apoyos

[![CI](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml/badge.svg)](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml)

Aplicación web para un centro de terapia ocupacional y apoyo educativo que atiende a personas autistas y a sus familias. El profesional diseña rutinas por pasos y se las asigna a cada participante; en la casa, el encargado registra día a día cómo salió cada paso; y el sistema calcula la adherencia en el tiempo para que la siguiente sesión se decida con datos y no de memoria.

SIRA es una herramienta de apoyo organizativo, no clínico. No guarda diagnósticos ni datos médicos, y "adherencia" se refiere al seguimiento de una rutina, no a un juicio sobre la persona.

Proyecto del curso **EIF509 Desarrollo de Aplicaciones Basadas en Web**, NRC 51092, Grupo G01, II Ciclo 2026. Universidad Nacional, Escuela de Informática. Profesor: Elberth Adrián Garro Sánchez.
Desarrollado por Anthony Cerdas Chacón (proyecto individual).

## Estado actual

Este repositorio tiene el esqueleto por capas del sistema. La lógica de negocio completa se construye en los laboratorios siguientes.

| Entregable del Lab 1 | Dónde está |
|----------------------|------------|
| Propuesta de dominio | [`docs/propuesta-dominio.md`](docs/propuesta-dominio.md) |
| Diagrama de arquitectura | [`docs/arquitectura.md`](docs/arquitectura.md) |
| Decisión de arquitectura (ADR) | [`docs/adr/ADR-001-arquitectura-en-capas.md`](docs/adr/ADR-001-arquitectura-en-capas.md) |
| Esqueleto Spring Boot por capas | [`src/main/java/cr/ac/una/sira/`](src/main/java/cr/ac/una/sira) |
| Integración continua | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |

## Stack

Java 21 (Temurin) · Spring Boot 3.3.13 · Gradle con wrapper · JUnit 5 y AssertJ · GitHub Actions.
A partir del Lab 2 entran PostgreSQL y MongoDB; el frontend React es del Lab 6.

## Requisitos

Solo Java 21. Verificalo con `java -version`, tiene que decir 21.x; si no lo tenés, está en [adoptium.net](https://adoptium.net). Todo lo demás lo descarga el wrapper de Gradle la primera vez.

## Cómo correrlo

```bash
./gradlew build
```

Compila y corre las pruebas. En Windows es `gradlew.bat build`, sin el `./`.

```bash
./gradlew bootRun
```

Levanta la aplicación en `http://localhost:8080`. Se detiene con `Ctrl + C`.

Con la app corriendo, desde otra terminal:

| Endpoint | Respuesta esperada |
|----------|--------------------|
| `GET /api/salud` | `{"estado":"OK - SIRA en linea"}` |
| `GET /api/participantes` | `[{"id":1,"nombre":"Participante A"},{"id":2,"nombre":"Participante B"}]` |
| `GET /actuator/health` | `{"status":"UP"}` |

```bash
curl http://localhost:8080/api/participantes
```

Si no tenés `curl`, pegá las URLs en el navegador.

Los datos de `/api/participantes` son una lista en memoria, porque en este laboratorio todavía no hay base de datos. Fijate que devuelve 2 de los 3 participantes: el tercero está inactivo y la regla de negocio lo excluye.

## Arquitectura

```
src/main/java/cr/ac/una/sira/
├── presentation/   Controladores REST y DTOs. Hablan HTTP.
├── business/       Servicios. Reglas, validaciones y cálculos.
├── data/           Repositorios y modelo. Leen y guardan datos.
└── config/         Configuración transversal (se llena en los próximos labs).
```

Las dependencias van en una sola dirección, `presentación -> negocio -> datos`, y nunca al revés. El diagrama completo, el recorrido de una petición y la justificación están en [`docs/arquitectura.md`](docs/arquitectura.md) y en el [ADR-001](docs/adr/ADR-001-arquitectura-en-capas.md).

## Pruebas

```bash
./gradlew test
```

`SiraApplicationTests` verifica que el contexto de Spring levanta y que todos los beans se inyectan. `ParticipanteServiceTest` verifica que la regla de negocio excluye inactivos y ordena por nombre; esta última corre sin levantar Spring, instanciando el servicio a mano, y eso solo es posible porque las capas están separadas.

## Integración continua

Cada push y cada pull request a `main` dispara [`.github/workflows/ci.yml`](.github/workflows/ci.yml), que en una máquina Linux limpia instala Java 21, compila y corre las pruebas. El resultado se ve en la pestaña Actions y en el badge de arriba.

Que el badge esté verde significa que el proyecto compila en cualquier máquina, no solo en la mía.

## Hoja de ruta

| Lab | Qué se agrega |
|-----|---------------|
| 1 | Esqueleto por capas, repositorio, CI, propuesta de dominio |
| 2 | PostgreSQL (6 entidades JPA) + MongoDB (bitácora de observaciones) |
| 3 | API REST completa: rutinas, pasos y ejecuciones |
| 4 | Cierre transaccional de la ejecución diaria |
| 5 | Seguridad y roles `PROFESIONAL` / `ENCARGADO` |
| 6 | Frontend SPA en React |
| 7 | Despliegue |

## Si algo falla

| Síntoma | Qué hacer |
|---------|-----------|
| `permission denied` con `./gradlew` | `chmod +x ./gradlew` en macOS o Linux |
| `JAVA_HOME not set` o versión vieja | Revisar con `java -version` que sea 21 |
| `port 8080 already in use` | Ya hay una app corriendo, cerrala con `Ctrl + C` |
| `Unable to establish loopback connection` | Bloqueo de sockets AF_UNIX en la máquina, no es del proyecto. Se arregla con `netsh winsock reset` en PowerShell como administrador, y reiniciando |

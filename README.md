# SIRA · Sistema de Rutinas y Apoyos

[![CI](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml/badge.svg)](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml)

Aplicación web empresarial para un centro de terapia ocupacional y apoyo educativo que atiende a
personas autistas y a sus familias. El profesional diseña **rutinas** por pasos y se las asigna a cada
participante; en casa, el encargado registra día a día cómo salió cada paso; el sistema calcula la
**adherencia** en el tiempo para que la siguiente sesión se decida con datos y no de memoria.

> SIRA es una herramienta de apoyo **organizativo**, no clínico. No almacena diagnósticos ni datos
> médicos, y el término *adherencia* describe el seguimiento de una rutina, no un juicio sobre la
> persona.

**Curso:** EIF509 Desarrollo de Aplicaciones Basadas en Web · NRC 51092 · Grupo G01 · II Ciclo 2026
**Universidad Nacional** · Escuela de Informática · Prof. Elberth Adrián Garro Sánchez
**Desarrollador:** Anthony Cerdas Chacón (proyecto individual)

---

## Estado actual · Laboratorio 1

Este repositorio contiene el **esqueleto por capas** del sistema. La lógica de negocio completa se
construye de forma incremental en los laboratorios siguientes.

| Entregable del Lab 1 | Dónde está |
|---|---|
| Propuesta de dominio (6 entidades, 2 procesos) | [`docs/propuesta-dominio.md`](docs/propuesta-dominio.md) |
| Diagrama de arquitectura | [`docs/arquitectura.md`](docs/arquitectura.md) |
| Decisión de arquitectura (ADR) | [`docs/adr/ADR-001-arquitectura-en-capas.md`](docs/adr/ADR-001-arquitectura-en-capas.md) |
| Esqueleto Spring Boot 3 por capas | [`src/main/java/cr/ac/una/sira/`](src/main/java/cr/ac/una/sira) |
| Integración continua | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |

---

## Stack

| | |
|---|---|
| Lenguaje | Java 21 (Eclipse Temurin) |
| Framework | Spring Boot 3.3.13 |
| Construcción | Gradle (wrapper incluido, no hace falta instalar Gradle) |
| Pruebas | JUnit 5 + AssertJ |
| CI | GitHub Actions |
| Persistencia | PostgreSQL + MongoDB *(a partir del Lab 2)* |
| Frontend | React SPA *(a partir del Lab 6)* |

---

## Requisitos

- **Java 21.** Verificalo con `java -version`; debe decir `21.x`.
  Si no lo tenés, descargá Eclipse Temurin 21 desde [adoptium.net](https://adoptium.net).
- Nada más. El wrapper de Gradle descarga todo lo demás la primera vez.

## Cómo correrlo

```bash
./gradlew build
```

Compila y ejecuta las pruebas. En Windows, usá `gradlew.bat build` (sin el `./`).

```bash
./gradlew bootRun
```

Levanta la aplicación en `http://localhost:8080`. Se detiene con `Ctrl + C`.

Con la aplicación corriendo, en otra terminal:

```bash
curl http://localhost:8080/api/salud
```

```bash
curl http://localhost:8080/api/participantes
```

```bash
curl http://localhost:8080/actuator/health
```

Respuestas esperadas:

| Endpoint | Respuesta |
|---|---|
| `GET /api/salud` | `{"estado":"OK - SIRA en linea"}` |
| `GET /api/participantes` | `[{"id":1,"nombre":"Participante A"},{"id":2,"nombre":"Participante B"}]` |
| `GET /actuator/health` | `{"status":"UP"}` |

> Si no tenés `curl`, pegá las URLs en el navegador: funcionan igual.

Los datos de `/api/participantes` son una lista en memoria: en el Lab 1 todavía no hay base de datos.
Fijate que devuelve **2** de los 3 participantes — el tercero está inactivo y la regla de negocio lo
excluye. Esa es justamente la demostración de que la capa de negocio hace algo propio.

---

## Arquitectura

```
src/main/java/cr/ac/una/sira/
├── presentation/   → Controladores REST y DTOs. Hablan HTTP. Sin lógica de negocio.
├── business/       → Servicios. Aquí viven las reglas, validaciones y cálculos.
├── data/           → Repositorios y modelo. Leen y escriben datos.
└── config/         → Configuración transversal de Spring (se llena en los próximos labs).
```

**Regla de oro:** las dependencias van en una sola dirección —
`presentación → negocio → datos`— y **nunca al revés**. Un controlador puede llamar a un servicio;
un servicio jamás debe saber que existe una pantalla.

El diagrama completo, el recorrido de una petición y la justificación están en
[`docs/arquitectura.md`](docs/arquitectura.md) y en el
[ADR-001](docs/adr/ADR-001-arquitectura-en-capas.md).

---

## Pruebas

```bash
./gradlew test
```

| Prueba | Qué verifica |
|---|---|
| `SiraApplicationTests` | El contexto de Spring levanta y todos los beans se inyectan |
| `ParticipanteServiceTest` | La regla de negocio excluye inactivos y ordena por nombre |

`ParticipanteServiceTest` corre **sin levantar Spring**: instancia el servicio a mano y le pasa el
repositorio por constructor. Eso solo es posible porque las capas están separadas.

---

## Integración continua

Cada `push` y cada pull request a `main` dispara [`.github/workflows/ci.yml`](.github/workflows/ci.yml),
que en una máquina Linux limpia instala Java 21, compila el proyecto y corre las pruebas. El resultado
se ve en la pestaña **Actions** del repositorio y en el badge de arriba.

Que el badge esté en verde significa que el proyecto compila **en cualquier máquina**, no solo en la
del desarrollador.

---

## Estructura del repositorio

```
sira/
├── .github/workflows/ci.yml     → Integración continua
├── docs/
│   ├── propuesta-dominio.md     → Dominio: 6 entidades y 2 procesos de negocio
│   ├── arquitectura.md          → Diagramas de capas y de secuencia
│   └── adr/                     → Architecture Decision Records
├── gradle/wrapper/              → Gradle wrapper (versionado a propósito)
├── src/main/java/               → Código de la aplicación, por capas
├── src/main/resources/          → application.properties
├── src/test/java/               → Pruebas
├── build.gradle                 → Dependencias y configuración de construcción
└── gradlew / gradlew.bat        → Lanzadores del wrapper
```

---

## Hoja de ruta

| Lab | Qué se agrega |
|---|---|
| **1** ✅ | Esqueleto por capas, repositorio, CI, propuesta de dominio |
| 2 | PostgreSQL (6 entidades JPA) + MongoDB (bitácora de observaciones) |
| 3 | API REST completa: rutinas, pasos y ejecuciones |
| 4 | Cierre transaccional de la ejecución diaria |
| 5 | Seguridad y roles `PROFESIONAL` / `ENCARGADO` |
| 6 | Frontend SPA en React |
| 7 | Despliegue |

---

## Solución de problemas

| Síntoma | Qué hacer |
|---|---|
| `permission denied` al correr `./gradlew` | `chmod +x ./gradlew` (macOS/Linux) |
| `JAVA_HOME not set` o versión vieja | Verificá con `java -version` que sea 21; reinstalá si hace falta |
| `port 8080 already in use` | Ya hay una app corriendo: cerrala con `Ctrl + C` |
| `Unable to establish loopback connection` al correr Gradle | Es un bloqueo de sockets AF_UNIX en la máquina (antivirus o Winsock dañado), no un problema del proyecto. Solución: `netsh winsock reset` en PowerShell **como administrador** y reiniciar |

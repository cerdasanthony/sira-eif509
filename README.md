# SIRA · Sistema de Rutinas y Apoyos

[![CI](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml/badge.svg)](https://github.com/cerdasanthony/sira-eif509/actions/workflows/ci.yml)

Aplicación web para un centro de terapia ocupacional que atiende a personas autistas y a sus familias. El profesional diseña rutinas por pasos y se las asigna a un participante; en la casa, el encargado registra cada día cómo salió cada paso; el sistema calcula la adherencia en el tiempo.

Proyecto del curso EIF509 Desarrollo de Aplicaciones Basadas en Web, NRC 51092, Grupo G01, II Ciclo 2026. Universidad Nacional. Anthony Cerdas Chacón, trabajo individual.

## Documentación

- [Propuesta de dominio](docs/propuesta-dominio.md)
- [Arquitectura](docs/arquitectura.md)
- [Laboratorio 2: capa de datos completa](docs/lab2-capa-datos.md)
- [Laboratorio 3: ORM, repositorios, consultas y N+1](docs/lab3-persistencia-orm.md)
- [Laboratorio 4: capa de negocio, transacciones, DTOs y pruebas](docs/lab4-capa-negocio.md)
- [Laboratorio 5: API REST, JWT, OpenAPI y pruebas](docs/lab5-api-rest.md)
- [Laboratorio 6: MVC, React y despliegue](docs/lab6-presentacion-despliegue.md)
- [Diagrama de despliegue](docs/despliegue.md)
- [Datos que completar para la entrega](docs/entrega-lab6.md)
- [Coleccion de peticiones REST](docs/sira-api.http)
- [ADR-001: organización en capas](docs/adr/ADR-001-arquitectura-en-capas.md)

## Stack

Java 21, Spring Boot 3.3.13, Spring Data JPA/MongoDB, Hibernate, Flyway,
PostgreSQL 16, MongoDB 7, Spring Security JWT, springdoc/Swagger UI, Bean Validation, Testcontainers, Gradle, JUnit 5,
Mockito, AssertJ y JaCoCo. Presentacion MVC con Thymeleaf y SPA con React/Vite.

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
./gradlew bootRun --args="--spring.profiles.active=demo"
```

Levanta la aplicación en el puerto 8080. Con la app corriendo:

El contrato completo, los permisos y los cuerpos de ejemplo estan en
[Laboratorio 5](docs/lab5-api-rest.md) y [sira-api.http](docs/sira-api.http).
Swagger UI: <http://localhost:8080/swagger-ui.html>.

| Endpoint | Respuesta |
|----------|-----------|
| `POST /auth/login` | JWT Bearer, valido 15 minutos |
| `GET /api/v1/usuarios/me` | Perfil autenticado |
| `GET /api/v1/participantes` | Pagina de participantes propios |
| `POST /api/v1/participantes` | 201 + Location |
| `GET /api/v1/rutinas` | Pagina con filtros de participante, estado y vigencia |
| `POST /api/v1/rutinas/{id}/publicaciones` | Publicacion validada por los servicios del Lab 4 |
| `POST /api/v1/ejecuciones/cierres` | Cierre transaccional, 201 + Location |
| `GET /api/v1/ejecuciones` | Historial propio por fechas y adherencia |
| `GET /actuator/health` | Estado de salud publico, sin detalles internos |

Con el perfil `demo`, iniciar sesion con `mariana.vargas@sira.local` (PROFESIONAL)
o `laura.mendez@sira.local` (ENCARGADO), password `SiraDemo2026!`.
Enviar `Authorization: Bearer <accessToken>` en cada llamada a `/api/v1`.
El perfil normal no configura contrasenas de demostracion.

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

Las pruebas unitarias con Mockito ejercitan los caminos felices y las reglas de
los dos procesos sin levantar Spring. Las pruebas de integracion usan
Testcontainers con PostgreSQL 16 real para validar persistencia y demostrar que
un fallo intermedio revierte por completo el cierre de una ejecucion. JaCoCo
genera el reporte en `build/reports/jacoco/test/html/index.html` y exige al menos
70 % de cobertura en la capa de negocio. La suite REST se ejecuta localmente
sobre H2 y tambien sobre PostgreSQL con Testcontainers cuando Docker esta
activo. Sin Docker se omiten las pruebas reales; la CI exige que no se omitan.

## Integración continua

Cada push a `main` dispara [`.github/workflows/ci.yml`](.github/workflows/ci.yml), que compila y corre las pruebas en una máquina Linux limpia con Java 21. El estado se ve en el badge de arriba.

## Laboratorio 6: ejecutar las dos interfaces

Requisitos adicionales: Node.js 22.12 o posterior y Docker para las bases de datos.

```bash
docker compose up -d
./gradlew bootRun --args="--spring.profiles.active=dev"
```

En otra terminal:

```bash
cd spa
npm ci
npm run dev
```

Administracion profesional: <http://localhost:8080/admin/login>.
SPA del encargado: <http://localhost:5173>. Las cuentas locales de demostracion
son las indicadas arriba. El perfil `prod` rechaza las credenciales de demostracion.
La sesion MVC requiere CSRF; la SPA envia JWT en Authorization y conserva el token
solo en memoria. Recargar la SPA requiere volver a ingresar.

## Variables de entorno de produccion

Configure los secretos en el panel de la plataforma. Esta tabla documenta solo
sus nombres y usos; no incluya sus valores en Git, capturas, logs ni archivos de entrega.

| Variable | Donde se configura | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | API | Seleccion del perfil de produccion |
| `PORT` | API, inyectado por el proveedor | Puerto HTTP del contenedor |
| `SIRA_POSTGRES_HOST` | API, desde PostgreSQL gestionado | Servidor PostgreSQL |
| `SIRA_POSTGRES_PORT` | API, desde PostgreSQL gestionado | Puerto PostgreSQL |
| `SIRA_POSTGRES_DB` | API, desde PostgreSQL gestionado | Nombre de la base |
| `SIRA_POSTGRES_USER` | API, desde PostgreSQL gestionado | Cuenta PostgreSQL |
| `SIRA_POSTGRES_PASSWORD` | API, secreto | Password PostgreSQL |
| `SIRA_POSTGRES_URL` | API, opcional | URL JDBC completa que sustituye host, puerto y base; debe usar TLS |
| `SIRA_MONGODB_URI` | API, secreto | Cadena Atlas con base, usuario y TLS |
| `SIRA_JWT_SECRET` | API, secreto | Clave aleatoria Base64 de al menos 32 bytes, exclusiva de produccion |
| `SIRA_CORS_ORIGINS` | API | Origen HTTPS real de la SPA, sin barra final; varios separados por coma |
| `SIRA_PROFESIONAL_PASSWORD` | API, secreto | Password inicial del profesional de prueba |
| `SIRA_ENCARGADO_PASSWORD` | API, secreto | Password inicial del encargado de prueba |
| `VITE_API_URL` | SPA, durante el build | Origen HTTPS publico de la API, sin ruta |

Las variables `VITE_*` son publicas en el JavaScript del navegador. Solo
`VITE_API_URL` se utiliza en la SPA. Las dos passwords iniciales se aplican una vez,
cuando las cuentas de ejemplo aun no tienen hash; cambiar estas variables despues
no cambia passwords ya guardadas. El encargado puede cambiar su password con
`PUT /api/v1/usuarios/me`; para el profesional se usa el mismo endpoint.

El [procedimiento de despliegue](docs/lab6-presentacion-despliegue.md#despliegue-en-render-y-atlas)
utiliza `Dockerfile` y `render.yaml`. El sistema se publico y verifico el 22 de
octubre de 2026:

- SPA: <https://sira-spa.onrender.com>
- API y salud: <https://sira-api-wzo1.onrender.com/actuator/health>
- Administracion: <https://sira-api-wzo1.onrender.com/admin/login>
- Swagger UI: <https://sira-api-wzo1.onrender.com/swagger-ui.html>

Las passwords de prueba de produccion se consultan en las variables privadas
del servicio API de Render y se entregan al docente fuera de Git. PostgreSQL
gratuito vence el 21 de noviembre de 2026. Los servicios gratuitos pueden tardar
en despertar despues de un periodo de inactividad.

# Laboratorio 5: API REST y servicios web

SIRA expone las entidades y los dos procesos del Laboratorio 4 en `/api/v1`.
Los controladores reciben DTOs validados y delegan en servicios transaccionales.
Las respuestas contienen DTOs: ninguna entidad JPA, relacion lazy ni hash se serializa.

## Ejecutar la demostracion

Requisitos: Java 21 y Docker Desktop con el motor Linux iniciado.

```powershell
docker compose up -d
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

Swagger UI: <http://localhost:8080/swagger-ui.html>. OpenAPI: <http://localhost:8080/v3/api-docs>.
La coleccion [sira-api.http](sira-api.http) se puede abrir con REST Client de VS Code.
Ejecutar los dos login primero; los siguientes bloques usan sus tokens automaticamente.

El perfil `demo` asigna `SiraDemo2026!` a las cuatro cuentas de ejemplo solo cuando
su hash es nulo. Mariana (`mariana.vargas@sira.local`, ID 1) y Carlos
(`carlos.brenes@sira.local`, ID 2) son PROFESIONAL. Laura
(`laura.mendez@sira.local`, ID 3) y Andres (`andres.solano@sira.local`, ID 4)
son ENCARGADO. Las cuentas de ejemplo se conservan del Lab 2.
El perfil normal no configura contrasenas de demostracion.

## Contrato y permisos

Todas las operaciones, salvo login, Swagger y Actuator health, requieren `Authorization: Bearer <token>`.
Las rutas antiguas sin version fueron reemplazadas.

| Metodo y ruta | Resultado | Permiso |
|---|---|---|
| POST /auth/login | 200, accessToken, tokenType y expiresIn | Publico |
| GET /api/v1/salud | 200 | Ambos roles |
| GET /api/v1/usuarios | 200, pagina | Cuentas visibles del actor |
| GET /api/v1/usuarios/me | 200, perfil propio | Ambos roles |
| GET /api/v1/usuarios/{id} | 200 | Propio, creado o encargado asignado |
| POST /api/v1/usuarios | 201 + Location | PROFESIONAL; crea ENCARGADO |
| PUT /api/v1/usuarios/me | 200 | Perfil propio, password opcional |
| DELETE /api/v1/usuarios/me | 204 | Desactiva cuenta propia; conserva historial |
| GET /api/v1/participantes | 200, pagina | Participantes asignados |
| GET /api/v1/participantes/{id} | 200 | Participante asignado |
| POST /api/v1/participantes | 201 + Location | PROFESIONAL; asignacion propia automatica |
| PUT /api/v1/participantes/{id} | 200 | PROFESIONAL asignado |
| DELETE /api/v1/participantes/{id} | 204 | PROFESIONAL asignado; sin rutinas |
| GET /api/v1/rutinas | 200, pagina | Propias; ENCARGADO ve PUBLICADA |
| GET /api/v1/rutinas/{id} | 200, detalle y pasos | Propia; ENCARGADO ve PUBLICADA |
| GET /api/v1/rutinas/{id}/pasos | 200, pasos ordenados | Igual que rutina |
| POST /api/v1/rutinas | 201 + Location | PROFESIONAL asignado; BORRADOR |
| PUT /api/v1/rutinas/{id} | 200 | PROFESIONAL asignado; BORRADOR |
| DELETE /api/v1/rutinas/{id} | 204 | PROFESIONAL asignado; BORRADOR |
| POST /api/v1/rutinas/{id}/publicaciones | 200, estado y duracion | PROFESIONAL asignado |
| GET /api/v1/ejecuciones | 200, pagina | Historial de participantes asignados |
| GET /api/v1/ejecuciones/{id} | 200, detalle y registros | Participante asignado |
| GET /api/v1/ejecuciones/{id}/registros | 200, resultados por paso | Participante asignado |
| POST /api/v1/ejecuciones/cierres | 201 + Location | ENCARGADO asignado |

`PasoRutina` se escribe como parte del agregado `Rutina`: el cuerpo de POST/PUT
contiene `pasos`. El servicio asigna el orden consecutivo desde 1. PUT reemplaza
los pasos dentro de una transaccion y solo en BORRADOR.
`RegistroPaso` se escribe como parte del cierre atomico de `Ejecucion`: se deben
enviar todos los resultados. El historial cerrado se consulta y no se sobrescribe.
La bitacora MongoDB del Lab 2 se mantiene como subdominio documental; este
laboratorio expone las seis entidades transaccionales de la propuesta.

## Paginacion, orden y Specifications

`page` empieza en 0; `size` vale 20 por defecto y admite hasta 100. `sort` acepta
`campo,asc` o `campo,desc`; se puede repetir. Se agrega `id` como desempate para
mantener paginas estables. Un campo de orden desconocido devuelve 400.

Las colecciones devuelven `contenido`, `pagina`, `tamano`, `totalElementos`,
`totalPaginas`, `primera` y `ultima`. El filtro de propiedad se combina siempre
con los filtros pedidos mediante Specifications y se aplica en SQL antes de paginar.

| Coleccion | Filtros opcionales | Orden permitido |
|---|---|---|
| usuarios | rol, activo | id, nombre, correo, rol, activo |
| participantes | activo, nombre (texto literal parcial) | id, nombre, fechaNacimiento, activo |
| rutinas | participanteId, estado, vigenteEn (YYYY-MM-DD) | id, nombre, horaInicio, vigenciaDesde, estado |
| ejecuciones | rutinaId, estado, desde, hasta, adherenciaMinima (0-100) | id, fecha, adherencia, estado, horaInicio |

Ejemplo: `GET /api/v1/rutinas?participanteId=1&estado=PUBLICADA&vigenteEn=2026-10-08&page=0&size=10&sort=horaInicio,asc`.
Pedir el ID de otro propietario en un filtro devuelve una pagina vacia; acceder
a ese recurso directamente devuelve 403.

## Semantica HTTP y errores RFC 9457

El unico `@RestControllerAdvice` traduce las excepciones. Los filtros de Spring
Security usan el mismo constructor de ProblemDetail, incluso antes de llegar a MVC.
El tipo de contenido es `application/problem+json`, con `type`, `title`, `status`,
`detail`, `instance` y `codigo`. La validacion agrega `errores` con campo y mensaje.
No se devuelven trazas, SQL, hashes, contrasenas ni valores rechazados.

| Estado | Caso |
|---|---|
| 201 + Location | Crear encargado, participante, borrador o ejecucion cerrada |
| 204 | Eliminar borrador/participante sin historial o desactivar cuenta propia |
| 400 | DTO invalido, JSON mal formado, enum/fecha/orden invalido, campo desconocido |
| 401 | Sin token, firma invalida, expiracion, credenciales invalidas o cuenta inactiva |
| 403 | Rol incorrecto o recurso de otro propietario |
| 405 + Allow | Metodo no permitido para esa ruta |
| 415 | Tipo de contenido no compatible |
| 404 | Recurso o referencia inexistente |
| 409 | Correo repetido, cierre diario duplicado, editar/publicar rutina ya publicada o borrar historial |
| 422 | Regla de negocio: encargado incorrecto, vigencia invertida, rutina incompleta, traslape o resultados incompletos |
| 500 | Error inesperado con mensaje generico; detalle solo en el registro del servidor |

## Flujo de seguridad

1. POST `/auth/login` compara BCrypt contra el hash en PostgreSQL.
2. `NimbusJwtEncoder` emite HS256 con `sub` igual al ID del usuario, `roles`,
   `iss=sira`, `aud=sira-api`, `iat` y `exp`; dura 15 minutos. La respuesta usa `Cache-Control: no-store`.
3. Resource Server valida firma, algoritmo y emisor; exige audiencia `sira-api` y expiracion presente y vigente.
4. SecurityFilterChain es STATELESS, sin sesion ni request cache; los permisos
   de ruta distinguen PROFESIONAL y ENCARGADO. CSRF esta desactivado porque
   las credenciales se envian como Bearer, no como cookie de sesion.
5. `AccesoRecursosService` revalida que la cuenta siga activa y conserve el rol,
   y los servicios comprueban la relacion profesional/encargado con el participante.
   Las listas tambien quedan limitadas al propietario.
6. Publicacion y cierre construyen los DTOs del Lab 4 con el actor autenticado.
   El cuerpo nunca permite escoger `profesionalId` ni `encargadoId` en estos procesos.

Para conservar tokens entre reinicios, configure `SIRA_JWT_SECRET` con Base64 de
al menos 32 bytes aleatorios. Sin esa variable, se crea una clave aleatoria por
arranque y los tokens previos quedan invalidos. No se guarda una clave en Git.
Los profesionales pueden crear encargados; el contrato no permite elegir ni
modificar roles desde un DTO. Las cuentas profesionales iniciales se provisionan
fuera del API publica. El creador puede consultar la nueva cuenta mediante su Location.

Referencias tecnicas: [Resource Server JWT de Spring Security](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html)
y [matriz de compatibilidad de springdoc](https://springdoc.org/v2/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).
Spring Boot 3.3 utiliza springdoc 2.6.0. Testcontainers se fija en 1.21.4,
cuya [publicacion oficial](https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4)
incluye compatibilidad con los motores Docker recientes.

## Pruebas y entrega

```powershell
.\gradlew.bat build
```

`ApiRestContrato` contiene una suite compartida con login y JWT reales, sin mocks
en controladores, filtros ni servicios. `ApiRestIntegracionTest` ejecuta esa suite
sobre PostgreSQL 16 con Testcontainers y las migraciones Flyway; `ApiRestLocalTest`
la ejecuta sobre H2 para desarrollo sin Docker. La fixture H2 no demuestra
compatibilidad PostgreSQL ni sustituye las migraciones o la prueba de rollback real.

Se verifican 201/Location, 204, 400, 401, 403, 404, 409 y 422; ambos procesos,
propiedad de participantes/rutinas/ejecuciones, colecciones filtradas y ordenadas,
congelamiento de pasos, identidad del JWT, hashes, cuentas inactivas y Swagger.
El Lab 4 conserva su prueba PostgreSQL de rollback con un trigger que fuerza
un fallo intermedio. Cada caso del contrato revierte sus datos al terminar.

Sin Docker, Testcontainers marca sus casos como omitidos. La CI exige que los
reportes de integracion existan y no contengan casos omitidos, ademas del minimo
70 % de cobertura de negocio. Los reportes locales estan en
`build/reports/tests/test/index.html` y `build/reports/jacoco/test/html/index.html`.

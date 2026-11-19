# Laboratorio 7: seguridad y robustez del sistema

Qué pide la guía y dónde está resuelto. La auditoría completa, con los hallazgos y sus
correcciones, está en [AUDITORIA-SEGURIDAD.md](../AUDITORIA-SEGURIDAD.md).

## Implementación

| Requisito de la guía | Implementación |
|---|---|
| Pipeline con verificación y despliegue (Sesión 13) | [`ci.yml`](../.github/workflows/ci.yml): trabajos `spa`, `build` y `desplegar`; el último solo corre con `push` sobre `main` y solo si los otros dos pasaron |
| Cobertura JaCoCo ≥ 70 % en negocio | `jacocoTestCoverageVerification` en `build.gradle` y paso propio en el pipeline |
| Secretos del pipeline en GitHub Secrets | `RENDER_DEPLOY_HOOK_API`, `RENDER_DEPLOY_HOOK_SPA` y `SIRA_HEALTH_URL`; el trabajo falla si falta alguno y ninguno se imprime |
| Protección de la rama `main` | Procedimiento y `gh api --input` en [AUDITORIA-SEGURIDAD.md](../AUDITORIA-SEGURIDAD.md#4-pipeline-y-protección-de-la-rama) con [`proteccion-main.json`](proteccion-main.json) |
| `AUDITORIA-SEGURIDAD.md` en la raíz | Los ocho puntos de la lámina 28 y las diez categorías del OWASP Top 10:2025, con hallazgo, riesgo, corrección y commit |
| `@Version` en la entidad con contención | `Rutina.version`, con la justificación en el README y en la sección 3 de la auditoría |
| Migración Flyway | [`V4__bloqueo_optimista_rutina.sql`](../src/main/resources/db/migration/V4__bloqueo_optimista_rutina.sql) |
| Conflicto como 409 en el manejador global | `ManejadorErroresApi.conflictoDeConcurrencia`, Problem Details con código `CONFLICTO_VERSION` |
| Prueba que provoca el conflicto | `BloqueoOptimistaRutinaTest`, `BloqueoOptimistaIntegracionTest` y las pruebas HTTP y MVC listadas en la auditoría |
| Actuator con `/actuator/health` sin detalles | `application.properties`; prueba `elChequeoDeSaludEsPublicoYSinDetallesYElRestoDeActuatorEstaCerrado` |
| Despliegue verificado contra la version nueva | `/actuator/info` publica `RENDER_GIT_COMMIT` y el pipeline espera ese SHA |
| Registros estructurados | [`logback-spring.xml`](../src/main/resources/logback-spring.xml), JSON a la salida estándar en el perfil `prod` |
| Eventos de seguridad registrados | `EventosSeguridad`: ingresos fallidos, cuentas en espera, accesos denegados y conflictos, sin passwords ni tokens |
| Al menos 40 pruebas en verde | 140 definidas (99 sin Docker, 41 con PostgreSQL); el pipeline falla si bajan de 40 o se omiten las de integracion |

## Archivos nuevos

| Archivo | Para qué |
|---|---|
| `AUDITORIA-SEGURIDAD.md` | Documento de auditoría exigido por la guía |
| `.github/dependabot.yml` | Punto 8 de la lista: revisión automática de dependencias |
| `docs/proteccion-main.json` | Configuración de la protección de `main` |
| `src/main/resources/db/migration/V4__bloqueo_optimista_rutina.sql` | Columna `version` de la rutina |
| `src/main/resources/logback-spring.xml` | Registros JSON en producción |
| `business/ControlIntentosIngreso.java` | Freno de fuerza bruta por cuenta y origen |
| `business/EventosSeguridad.java` | Bitácora de seguridad con campos y sin secretos |
| `business/exception/ConflictoVersionException.java` | Copia vencida del formulario |
| `business/exception/DemasiadosIntentosException.java` | Cuenta en espera, se traduce a 429 |
| `config/ProveedorIngresoLimitado.java` | Aplica el freno al formulario MVC |
| `data/BloqueoOptimistaRutinaTest.java` | Conflicto de versión sobre H2, tres pruebas |
| `data/BloqueoOptimistaIntegracionTest.java` | El mismo conflicto sobre PostgreSQL real |
| `business/ControlIntentosIngresoTest.java` | Cinco pruebas del freno de intentos |
| `business/EventosSeguridadTest.java` | Cinco pruebas de los eventos y del JSON |
| `presentation/ManejadorErroresApiTest.java` | 409 y 429 con Problem Details |
| `src/test/resources/concurrencia-limpiar.sql` | Base limpia para la suite de concurrencia |

## Cómo verlo funcionando

### El conflicto de concurrencia, a mano

1. Levantar las bases y la aplicación:

```bash
docker compose up -d
```

```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```

2. Ingresar como profesional en `http://localhost:8080/admin/login` y crear una rutina.
3. Abrir el mismo borrador en dos pestañas con **Editar**.
4. Guardar en la primera pestaña: funciona y vuelve al listado.
5. Guardar en la segunda, que todavía tiene la versión anterior: el formulario se vuelve
   a mostrar con los datos intactos y el aviso de que otra persona guardó mientras tanto.
6. Lo mismo por la API, con el detalle de la rutina a la vista para leer `version`:

```bash
curl -i -X PUT "http://localhost:8080/api/v1/rutinas/10" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"participanteId":1,"nombre":"Rutina","horaInicio":"10:00:00","vigenciaDesde":"2026-10-01","vigenciaHasta":null,"diasSemana":[1,2],"pasos":[{"descripcion":"Paso","duracionEstimadaMin":3,"pictograma":null}],"version":0}'
```

La segunda llamada con la misma `version` responde `409` y
`{"codigo":"CONFLICTO_VERSION", ...}` en `application/problem+json`.

Guardar o publicar sin `version`, o con una version negativa, se rechaza: 400 en la
API, error de formulario o aviso en MVC. Crear no requiere version. La comprobacion
vive en el servicio para que las dos interfaces apliquen la misma regla.

### Despliegue despues de la CI

`render.yaml` desactiva los despliegues por push con `autoDeployTrigger: 'off'` en
ambos servicios. GitHub Actions llama a los hooks con `ref=github.sha`: asi solicita
el commit verificado aunque la rama ya haya avanzado.

En servicios ya creados hay que sincronizar el Blueprint o configurar **Auto-Deploy:
Off** en ambos servicios. El 19 de noviembre se configuro y verifico **Off** en el
panel de los servicios API y SPA existentes. Las referencias de la plataforma son
[autoDeployTrigger](https://render.com/docs/blueprint-spec#autodeploytrigger) y
[deploy hooks con commit especifico](https://render.com/docs/deploy-hooks#deploying-a-specific-git-commit).

### El freno de intentos

```bash
for i in 1 2 3 4 5 6; do curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"mariana.vargas@sira.local","password":"incorrecta"}'; done
```

Las cinco primeras responden `401` y la sexta `429` con la cabecera `Retry-After`.

### Las cabeceras y el chequeo de salud

```bash
curl -i http://localhost:8080/actuator/health
```

Responde `{"status":"UP"}`, sin componentes ni detalles, con `Content-Security-Policy`,
`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y `Permissions-Policy`.

### Los registros en JSON

```bash
./gradlew bootRun --args="--spring.profiles.active=prod"
```

Requiere las variables de producción. Cada línea sale como un objeto JSON; los eventos de
seguridad llevan el logger `cr.ac.una.sira.seguridad` y sus campos en `kvpList`.

## Verificación

```bash
./gradlew build
```

```bash
cd spa && npm ci && npm test && npm run build
```

En Windows, si `gradlew` falla por los sockets de dominio Unix, la verificación de
referencia es la de GitHub Actions, que corre la suite completa con Docker.

### Resultado local del 27 de octubre de 2026

- Compilación de `src/main/java` y `src/test/java` con `javac 21.0.12`: correcta.
- Suite sin Docker, ejecutada con el JUnit Platform Launcher: 96 pruebas, 96 en verde,
  ninguna omitida.
- Las cuatro suites que necesitan Docker (`ApiRestIntegracionTest`,
  `PersistenciaIntegracionTest`, `CierreEjecucionRollbackIntegracionTest` y
  `BloqueoOptimistaIntegracionTest`, 39 pruebas) no se ejecutaron en esta máquina: las
  corre la integración continua, que además rechaza que se omitan.
- `render.yaml`, `ci.yml`, `dependabot.yml` y `proteccion-main.json`: sintaxis válida.

### Resultado local del 19 de noviembre de 2026

- `gradlew.bat build --no-daemon`, Java 21.0.13 y Gradle 9.5.1: correcto, incluido
  `jacocoTestCoverageVerification`.
- Backend: 140 pruebas descubiertas, 99 aprobadas, cero fallos y 41 omitidas por
  falta de Docker. Incluye las pruebas de seguridad, concurrencia con H2, la nueva
  comprobacion de version obligatoria en API/MVC y los eventos de conflicto MVC.
- Cobertura de lineas: 98,4 % en `cr.ac.una.sira.business`; DTOs y excepciones, 100 %.
- SPA: `npm ci`, `npm test` (23 aprobadas) y `npm run build`, correctos; la instalacion
  informo cero vulnerabilidades conocidas.
- `ci.yml` y `render.yaml`: sintaxis validada con SnakeYAML; `git diff --check`, correcto.
- Las 41 pruebas de PostgreSQL deben ejecutarse en la CI al publicar estos cambios;
  no se presentan como aprobadas en esta verificacion local.
- `main` protegida segun la API publica de GitHub. Auto-Deploy cambiado de On Commit
  a Off y guardado en los servicios API y SPA de Render. El despliegue de esta revision
  sigue pendiente de publicar la rama y pasar la CI.

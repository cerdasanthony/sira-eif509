# Auditoría de seguridad · SIRA

Laboratorio 7 de EIF509 Desarrollo de Aplicaciones Basadas en Web, NRC 51092, Grupo G01,
II Ciclo 2026. Universidad Nacional. Anthony Cerdas Chacón, trabajo individual.

Revisión del sistema desplegado del Laboratorio 6 con la lista de verificación de la
Sesión 14 (lámina 28) y con el OWASP Top 10:2025. Cada fila indica qué se revisó, qué se
encontró, qué riesgo representa, qué corrección se aplicó y en qué commit está. Donde no
hubo hallazgo se indica cómo se comprobó, para que la revisión se pueda repetir.

Este documento no contiene claves, tokens, URL de deploy hooks ni ningún otro dato
sensible real: solo nombres de variables y rutas de archivos.

Todas las correcciones viajan en el commit [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27); su contenido esta detallado en
[Commits de las correcciones](#commits-de-las-correcciones).

## 1. Lista de verificación de la Sesión 14

| # | Verificación | Hallazgo | Riesgo | Corrección aplicada | Commit |
|---|---|---|---|---|---|
| 1 | Un usuario no puede ver ni modificar recursos de otro | **Sin hallazgo.** `AccesoRecursosService` compara el dueño del recurso con el usuario del JWT en cada operación; el id del encargado nunca se toma del cuerpo de la petición. Comprobado cambiando el identificador en la URL con el token del otro rol: responde 403 con Problem Details. Pruebas: `rolesYPropiedadImpidenAccesoHorizontal403`, `recursosInexistentesDevuelven404YNoFiltranEntidades`, `vistasPaginadasReutilizanPropiedadYLaSesionNoAutenticaLaApi` | — | Se agregó el registro del evento `acceso_denegado` para poder detectar los intentos (ver fila 8 de la sección 2) | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| 2 | Ningún endpoint devuelve una entidad JPA | **Sin hallazgo.** Los tipos de retorno de los nueve controladores son únicamente DTO del `ContratoApi` (`UsuarioSalida`, `ParticipanteSalida`, `RutinaSalida`, `EjecucionSalida`, `Pagina<…>`) o `ResponseEntity<DTO>`. Comprobado revisando las firmas de `src/main/java/cr/ac/una/sira/presentation/*Controller.java` y con la prueba `loginEmiteTokenYNoCreaSesionNiExponePassword`, que verifica que `passwordHash` no aparece en la respuesta | — | — | — |
| 3 | No hay consultas construidas concatenando texto | **Sin hallazgo.** Las cinco consultas `@Query` usan JPQL con parámetros `:nombre`; los filtros dinámicos usan `Specification` con `CriteriaBuilder`. El único `+` cercano a una consulta arma el patrón de un `LIKE` que viaja como parámetro y que además escapa `\`, `%` y `_`, de modo que el cliente no puede aportar comodines. Comprobado con `grep -rn "createQuery\|createNativeQuery\|@Query" src/main/java` y con la prueba `paginacionOrdenYFiltrosSeAplicanEnBdConAlcanceDelPropietario`, que envía `nombre=%` y obtiene cero resultados | — | — | — |
| 4 | Los errores no envían la traza al cliente | **Sin hallazgo en el formato, un hallazgo en una excepción nueva.** `server.error.include-stacktrace=never` e `include-message=never` ya estaban, y el `@RestControllerAdvice` traduce todo a Problem Details. Al agregar el bloqueo optimista, `ObjectOptimisticLockingFailureException` habría caído en el manejador genérico y habría salido como 500 | Medio: un conflicto entre dos usuarios se vería como un fallo del servidor y mandaría a revisar el log por un caso que no es un error del sistema | Manejador propio que la traduce a 409 con Problem Details y código `CONFLICTO_VERSION`, en `ManejadorErroresApi`. Se reforzó `server.error.include-binding-errors=never` e `include-exception=false`. Pruebas: `ManejadorErroresApiTest`, `guardarOPublicarConUnaVersionVencidaDevuelve409ConProblemDetails` | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| 5 | CORS solo permite el dominio de la SPA | **Sin hallazgo.** `corsConfigurationSource` toma los orígenes de `SIRA_CORS_ORIGINS`, nunca `*`, con `allowCredentials=false` y solo las cabeceras `Authorization` y `Content-Type`. `ProduccionConfig` rechaza el arranque si la variable está vacía o si algún origen no es HTTPS exacto sin ruta. Comprobado con la prueba `corsAceptaSoloOrigenConfigurado`, que verifica que un origen ajeno no recibe `Access-Control-Allow-Origin` | — | — | — |
| 6 | No hay secretos en el repositorio ni en su historial | **Sin hallazgo.** Comprobado con `git log --all -p \| grep -i -E "password\|secret\|token\|api[_-]?key\|BEGIN .*PRIVATE\|srv://"`: las únicas coincidencias son nombres de variables, la clase `SecretKey` de Java y las passwords de demostración que solo existen en los datos de prueba. `.gitignore` cubre `.env`, `.env.*`, `tmp/` y los artefactos locales. Los valores reales viven en el panel de la plataforma y en GitHub Secrets | — | Se agregó `.codex-remote-attachments/` al `.gitignore` para que una carpeta de herramienta local no entre por descuido | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| 7 | Las claves se guardan con BCrypt y el JWT expira | **Sin hallazgo.** `PasswordEncoder` es `BCryptPasswordEncoder`; la prueba `crearEncargado201PasswordHasheadoYCorreoDuplicado409` comprueba que el hash almacenado empieza por `$2a$`. El JWT se emite con `exp` a 15 minutos, `iss`, `aud` y `sub`; `tokenExpiradoEmisorOAudienciaIncorrectosDevuelven401` verifica que un token vencido, de otro emisor o de otra audiencia recibe 401, y `tokenSinAudienciaOSinExpiracionDevuelve401` que uno sin `exp` también. `ProduccionConfig` exige una clave de al menos 32 bytes aleatorios | — | — | — |
| 8 | Las dependencias no tienen vulnerabilidades conocidas | **Hallazgo.** El repositorio no tenía Dependabot configurado y el pipeline no revisaba las dependencias publicadas de la SPA; una dependencia vulnerable podía quedarse en producción sin que nadie se enterara | Medio: corresponde a A03 Fallas en la cadena de suministro; el riesgo concreto depende de la dependencia afectada | `.github/dependabot.yml` con revisión semanal de Gradle y npm y mensual de GitHub Actions y Docker, y un paso `npm audit --omit=dev --audit-level=high` en el trabajo `spa` del pipeline, que corta el despliegue si aparece una vulnerabilidad alta o crítica | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |

## 2. OWASP Top 10:2025

Categorías revisadas una por una contra el sistema. Fuente: OWASP Foundation (2025),
*OWASP Top 10:2025*, tal como se presenta en la lámina 6 de la Sesión 14.

| Categoría | Qué se revisó en SIRA | Hallazgo y riesgo | Corrección aplicada | Commit |
|---|---|---|---|---|
| A01 Control de acceso roto | Propiedad del recurso en API y MVC, separación de roles PROFESIONAL/ENCARGADO, acceso del encargado solo a rutinas publicadas, aislamiento entre la sesión MVC y el JWT | **Sin hallazgo.** Ver fila 1 de la sección 1. La sesión MVC no autentica `/api/v1` y el JWT no abre `/admin`; las dos reglas tienen prueba | — | — |
| A02 Configuración incorrecta de seguridad | Cabeceras de respuesta, Actuator, Swagger, CORS, cookies de sesión, límites de la petición | **Hallazgo.** Faltaban `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy` y un `Strict-Transport-Security` explícito; `/actuator` publicaba el índice de endpoints; no había límite de tamaño de cuerpo ni de cabeceras. Riesgo medio: sin CSP, cualquier inyección futura en una vista podría cargar y ejecutar código de otro dominio, y una petición enorme podía agotar la memoria del contenedor gratuito | `cabecerasComunes` en `SeguridadConfig` aplica HSTS de un año con subdominios, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: strict-origin-when-cross-origin` y `Permissions-Policy` sin cámara, micrófono, ubicación, pago ni USB. CSP por cadena: `default-src 'none'` en la API, `default-src 'self'` sin `unsafe-inline` en el panel MVC y una política propia para Swagger UI, que sí necesita estilos en línea. En `render.yaml`, CSP, HSTS y las mismas cabeceras para la SPA. `management.endpoints.web.discovery.enabled=false`, límites de tamaño en `application.properties` y, de Actuator, solo `health` e `info`. Pruebas: `todaRespuestaLlevaLasCabecerasDeSeguridadYLaApiNoPermiteRecursosExternos`, `elPanelMvcSirveUnaPoliticaDeContenidoEstrictaYLasDemasCabeceras`, `elChequeoDeSaludEsPublicoYSinDetallesYElRestoDeActuatorEstaCerrado` | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| A03 Fallas en la cadena de suministro | Dependencias de Gradle y npm, acciones del pipeline, imagen base del Dockerfile | **Hallazgo.** Ver fila 8 de la sección 1 | Dependabot y `npm audit` en el pipeline | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| A04 Fallas criptográficas | Almacenamiento de passwords, clave y algoritmo del JWT, TLS en tránsito y hacia la base de datos | **Sin hallazgo.** BCrypt para las passwords; HS256 con clave de 32 bytes o más tomada de `SIRA_JWT_SECRET`; `sslmode=require` en la URL JDBC de producción; TLS en la cadena de Atlas; cookies de sesión `Secure`, `HttpOnly` y `SameSite=Lax`. El login compara siempre contra un hash, incluso cuando la cuenta no existe, para no revelar por tiempo qué correos están registrados | Se reforzó con HSTS, que impide la primera conexión en texto plano (ver A02) | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| A05 Inyección | Consultas JPQL y Criteria, escapado en las vistas Thymeleaf y React | **Sin hallazgo.** Ver fila 3 de la sección 1. Thymeleaf escapa con `th:text` y no hay un solo `th:utext` en las plantillas; React no usa `dangerouslySetInnerHTML`. La CSP agregada en A02 limita el daño de una inyección futura | — | — |
| A06 Diseño inseguro | Dónde se validan las reglas de negocio y qué pasa cuando dos personas editan lo mismo | **Hallazgo.** Las reglas sí se validan en el servidor, pero la rutina no estaba protegida contra actualizaciones perdidas: el panel MVC y la publicación escriben la misma fila, y si el profesional tenía el formulario abierto en dos ventanas, la segunda escritura pisaba la primera sin avisar a nadie. Riesgo alto para el dominio: el encargado podría recibir una rutina publicada distinta de la que se revisó | `@Version` en `Rutina`, migración `V4__bloqueo_optimista_rutina.sql`, comprobación de la versión que trae el formulario en `RutinaApiService` y traducción del conflicto a 409. Detalle en la sección 3 | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| A07 Fallas de autenticación | Expiración del token, revocación al desactivar la cuenta, límite de intentos de ingreso | **Hallazgo.** El token expira y la cuenta desactivada deja de servir, pero ni `/auth/login` ni `/admin/login` limitaban los intentos: una lista de passwords comunes se podía probar entera contra un correo conocido. Riesgo alto: es la puerta de entrada al sistema y los correos del dominio son predecibles | `ControlIntentosIngreso` (cinco fallos por cuenta y origen dejan la cuenta quince minutos en espera) y `ProveedorIngresoLimitado`, que aplica el mismo freno al formulario MVC. La API responde 429 con `Retry-After` y Problem Details; el panel MVC redirige a `/admin/login?bloqueado` con un aviso. Pruebas: `ControlIntentosIngresoTest`, `cincoIngresosFallidosDejanLaCuentaEnEsperaConRespuesta429`, `cincoIngresosFallidosEnElPanelDejanLaCuentaEnEspera` | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| A08 Fallas de integridad de software o datos | Origen de lo que se despliega, deserialización, service worker de la PWA | **Sin hallazgo.** El despliegue solo se dispara desde `main` y solo después de que la compilación, las pruebas y la cobertura pasaron; los deploy hooks viven en GitHub Secrets. No se deserializa nada que no sea JSON validado contra un DTO, y `fail-on-unknown-properties=true` rechaza los campos que no pertenecen al contrato. El service worker guarda solo la interfaz pública: ni respuestas de la API ni credenciales | Se agregó la protección de la rama `main` (ver sección 4) y la verificación del chequeo de salud después de desplegar | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| A09 Fallas en el registro y las alertas | Qué queda registrado cuando alguien falla al ingresar, cuando se le niega un acceso o cuando hay un conflicto | **Hallazgo.** No se registraba ninguno de los tres. Sin esos registros no hay forma de notar un intento de fuerza bruta ni de reconstruir qué pasó en producción. Riesgo medio-alto: no abre una vulnerabilidad por sí mismo, pero deja ciego al sistema ante las demás | `EventosSeguridad` publica `ingreso_fallido`, `ingreso_bloqueado`, `ingreso_correcto`, `acceso_denegado`, `autenticacion_requerida` y `conflicto_version` como pares clave/valor, y `logback-spring.xml` los escribe en JSON a la salida estándar en producción. Nunca salen passwords, hashes ni tokens: del correo solo se publican dos letras y el dominio. Pruebas: `EventosSeguridadTest` | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |
| A10 Manejo inadecuado de condiciones excepcionales | Qué devuelve el servidor ante un duplicado, un dato inválido o un fallo interno | **Hallazgo menor.** Ver fila 4 de la sección 1: el conflicto de concurrencia habría salido como 500 con el manejador genérico | Manejador propio para `OptimisticLockingFailureException` y para `DemasiadosIntentosException`, ambos con Problem Details y sin traza | [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) |

## 3. Concurrencia: por qué la rutina y no otra cosa

La operación con contención real del dominio es **guardar o publicar una rutina**.
El profesional edita el borrador desde el panel MVC y lo publica desde el listado; el
mismo borrador puede estar abierto en dos ventanas, o puede estar trabajándolo un
profesional suplente. Las dos operaciones escriben la misma fila de `rutina`, y la
transacción por sí sola no evita que la segunda escritura pise a la primera: ambas leen
la versión anterior y ambas guardan. El resultado es que el encargado recibe una rutina
publicada que nadie revisó en esa forma.

Lo que se hizo:

- `@Version private long version` en `Rutina` y la migración
  `V4__bloqueo_optimista_rutina.sql`, que agrega la columna con `DEFAULT 0 NOT NULL`.
- El detalle y el listado de rutinas exponen `version`; el formulario MVC la lleva en un
  campo oculto y el botón de publicar la envía como parámetro.
- `RutinaApiService` compara la versión que trae el cliente con la almacenada y lanza
  `ConflictoVersionException` si no coinciden. La carrera que ocurre *después* de esa
  comprobación la detecta Hibernate al escribir, porque el `UPDATE` lleva
  `WHERE id = ? AND version = ?` y afecta cero filas.
- Las dos situaciones terminan en un **409 con Problem Details** y el código
  `CONFLICTO_VERSION`, y quedan registradas como evento de seguridad.

Pruebas que provocan el conflicto:

| Prueba | Qué demuestra |
|---|---|
| `BloqueoOptimistaRutinaTest.dosSesionesQueGuardanElMismoBorradorALaVezSoloDejanPasarALaPrimera` | Dos unidades de trabajo abiertas a la vez; la primera confirma y la segunda recibe `OptimisticLockException` |
| `BloqueoOptimistaRutinaTest.guardarUnaCopiaVencidaDesdeElRepositorioSeTraduceAErrorDeConcurrenciaDeSpring` | `saveAndFlush` de una copia vencida lanza `ObjectOptimisticLockingFailureException` |
| `BloqueoOptimistaRutinaTest.cadaGuardadoCorrectoSubeLaVersionEnUno` | La versión avanza en cada escritura |
| `BloqueoOptimistaIntegracionTest` | El mismo conflicto contra PostgreSQL real, sobre la columna que crea la migración V4 |
| `ApiRestContrato.guardarOPublicarConUnaVersionVencidaDevuelve409ConProblemDetails` | El conflicto llega al cliente como 409 `application/problem+json` con código `CONFLICTO_VERSION` |
| `MvcLocalTest.elFormularioDeRutinaLlevaLaVersionYRechazaGuardarUnaCopiaVencida` | En el panel MVC el formulario conserva la versión y el guardado vencido muestra el aviso sin perder los datos |
| `ManejadorErroresApiTest` | El manejador global traduce el fallo de bloqueo optimista a 409 |

Ninguna prueba corre dentro de una transacción de prueba: si así fuera, las dos lecturas
devolverían el mismo objeto y no habría conflicto que observar.

## 4. Pipeline y protección de la rama

`.github/workflows/ci.yml` tiene tres trabajos:

1. `spa`: instala, prueba y compila el frontend React y corre `npm audit`.
2. `build`: compila el backend, corre toda la suite, exige la cobertura mínima de JaCoCo
   (70 % de líneas en `cr.ac.una.sira.business`), exige que las pruebas de integración con
   PostgreSQL real no se omitan y exige al menos 40 pruebas en verde.
3. `desplegar`: solo con `github.ref == 'refs/heads/main'` y `github.event_name == 'push'`,
   y solo si los dos trabajos anteriores pasaron. Llama a los deploy hooks de la
   plataforma y espera a que `/actuator/health` responda `UP`.

Secretos del pipeline, configurados en **GitHub Secrets** (aquí solo sus nombres):

| Secreto | Uso |
|---|---|
| `RENDER_DEPLOY_HOOK_API` | URL privada que dispara el despliegue del servicio de la API |
| `RENDER_DEPLOY_HOOK_SPA` | URL privada que dispara el despliegue del sitio estático de la SPA |
| `SIRA_HEALTH_URL` | URL pública de `/actuator/health`; de ella sale también la de `/actuator/info` |

El trabajo falla de inmediato si falta alguno, y ninguna de las tres se imprime en el log.

**Protección de la rama `main`** (Settings → Branches → Add branch ruleset), con la
configuración que exige el laboratorio:

- Require a pull request before merging, con al menos una aprobación.
- Require status checks to pass before merging: `Compilar y probar` y `Probar y compilar SPA`.
- Require branches to be up to date before merging.
- Block force pushes y Restrict deletions.

Equivalente por línea de comandos:

```bash
gh api -X PUT repos/cerdasanthony/sira-eif509/branches/main/protection \
  --input docs/proteccion-main.json
```

## 5. Observabilidad

- De Actuator solo se exponen dos endpoints, con el índice desactivado.
  `/actuator/health` va con `show-details=never` y `show-components=never`: devuelve
  `{"status":"UP"}` y nada más. La plataforma lo usa como health check.
- `/actuator/info` publica un único dato, `despliegue.commit`, que es el SHA que la
  plataforma inyecta en `RENDER_GIT_COMMIT`. Hace falta porque un `UP` a secas no
  prueba nada: mientras la plataforma reconstruye, la instancia anterior sigue en pie
  y responde que todo está bien, así que el pipeline daría por desplegada una versión
  que todavía no existe. Esperando ese commit, el despliegue queda verificado de
  verdad. El repositorio es público, de modo que el SHA no revela nada que no esté ya
  en GitHub, y la prueba `infoPublicaSoloElCommitDesplegadoParaQueLaCiVerifiqueLaVersion`
  comprueba que el contribuidor de entorno no arrastre ningún otro dato.
- Los registros salen a la salida estándar. En producción, `logback-spring.xml` los
  escribe en JSON con el `JsonEncoder` de Logback, con nivel, logger, mensaje y los pares
  clave/valor de cada evento; en desarrollo se conserva el formato legible de Spring Boot.
  La lámina 25 propone `logging.structured.format.console=ecs`, que requiere Spring
  Boot 3.4 o posterior; este proyecto está en 3.3.13, así que se usa el encoder de Logback,
  que produce el mismo JSON por línea sin agregar dependencias.
- Eventos de seguridad registrados: `ingreso_fallido`, `ingreso_bloqueado`,
  `ingreso_correcto`, `acceso_denegado`, `autenticacion_requerida` y `conflicto_version`.
  Ninguno incluye passwords, hashes ni tokens, y el correo se publica enmascarado
  (`ma***@sira.local`). `EventosSeguridadTest` lo verifica.

## 6. Conjunto de pruebas

| Suite | Pruebas | Dónde corre |
|---|---|---|
| Reglas de negocio y configuración (unitarias, Mockito) | 37 | Siempre |
| `ControlIntentosIngresoTest`, `EventosSeguridadTest`, `ManejadorErroresApiTest` | 13 | Siempre |
| `BloqueoOptimistaRutinaTest` | 3 | Siempre (H2) |
| `MvcLocalTest` | 15 | Siempre (H2) |
| `ApiRestLocalTest` | 28 | Siempre (H2) |
| `ApiRestIntegracionTest` | 28 | Con Docker; obligatorio en la CI |
| `PersistenciaIntegracionTest` | 8 | Con Docker; obligatorio en la CI |
| `CierreEjecucionRollbackIntegracionTest` | 1 | Con Docker; obligatorio en la CI |
| `BloqueoOptimistaIntegracionTest` | 2 | Con Docker; obligatorio en la CI |
| **Total en la CI** | **135** | |

Sin Docker se ejecutan 96; el pipeline exige al menos 40 en verde y rechaza que las
pruebas de integración se omitan.

## Commits de las correcciones

Las correcciones del Laboratorio 7 se confirmaron juntas, porque se sostienen entre si:
el conflicto de concurrencia necesita el manejador de errores y el evento de seguridad,
y el despliegue automatico solo tiene sentido con la verificacion que lo precede.

| Commit | Contenido |
|---|---|
| [`feda6ef`](https://github.com/cerdasanthony/sira-eif509/commit/feda6ef32535c2b94df08afcb113abc9faf27b27) | Cabeceras de seguridad y CSP por cadena, limite de intentos de ingreso con 429, limites de tamano de la peticion y Actuator sin indice; `@Version` en la rutina con la migracion V4 y el conflicto como 409 con Problem Details; eventos de seguridad y registros JSON; pipeline con despliegue automatico, Dependabot y revision de dependencias |

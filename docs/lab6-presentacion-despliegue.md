# Laboratorio 6: presentacion y despliegue

## Implementacion

Las dos interfaces llaman a la misma capa `business`. Los controladores MVC no
acceden a repositorios ni hacen llamadas HTTP a la propia API.

| Requisito | Implementacion |
|---|---|
| MVC con al menos dos casos de uso | Alta de encargados, alta/edicion de participantes y creacion/edicion/publicacion de rutinas |
| Listados paginados | `/admin/encargados`, `/admin/participantes`, `/admin/rutinas`, 10 filas por pagina |
| Validacion y BindingResult | DTOs con Bean Validation y comprobaciones del negocio; errores visibles conservando los datos no sensibles |
| Post/Redirect/Get | Cada alta, edicion o publicacion exitosa redirige al listado con mensaje flash |
| CSRF | Cadena `/admin/**` con CSRF activo; Thymeleaf agrega el token a los formularios POST, incluido login/logout |
| React en `/spa` | Login JWT, rutinas paginadas, carga/error/reintento/vacio, formulario de cierre y consulta de historial |
| Cliente centralizado y 401 | `spa/src/api.js`: Authorization Bearer, errores ProblemDetail y expiracion que devuelve al login |
| Proceso principal | Encargado elige rutina publicada, fecha/horario, resultado de cada paso y observacion opcional; backend cierra y calcula adherencia |
| Perfiles | `application-dev.properties`, `application-prod.properties`; la produccion rechaza perfiles demo/dev y clave JWT vacia |
| Nube | Dockerfile multietapa, Blueprint de Render para API/SPA/PostgreSQL; MongoDB Atlas se configura por variable |
| Diagrama | [despliegue.md](despliegue.md) |

## Flujo de demostracion

1. Ingresar como profesional en `/admin/login`.
2. Crear un encargado y anotar su ID, o usar la cuenta de prueba configurada.
3. Crear un participante con ese encargado. Los listados solo muestran recursos
   que pertenecen al profesional autenticado.
4. Crear una rutina para el participante. Incluir dias, vigencia y pasos con el
   formato `descripcion | minutos`, uno por linea. Guardar el borrador.
5. Publicar desde el listado de rutinas. Las reglas del laboratorio 4 comprueban
   integridad, permisos y traslapes antes de cambiar el estado.
6. Ingresar con el encargado en la SPA. Solo vera sus rutinas publicadas.
7. Abrir la rutina y registrar todos los pasos para una fecha de su programacion.
   Al guardar se muestra la adherencia calculada; Mis registros consulta el historial.
8. Si el servidor rechaza la fecha o ya existe un cierre, se muestra el error y se
   mantienen las selecciones para corregirlo. No se selecciona un resultado por defecto.

El servidor revalida la cuenta, el rol y la propiedad en cada operacion. El id
del encargado procede del JWT/sesion, nunca de un campo editable del navegador.
El token de la SPA vive en memoria; su expiracion se detecta al recibir 401. La
sesion MVC no autentica endpoints API y la SPA no envia cookies al backend.

## Despliegue en Render y Atlas

Estado: desplegado en Render y Atlas y verificado el 22 de octubre de 2026.
Las URLs y la evidencia de las comprobaciones estan en
[entrega-lab6.md](entrega-lab6.md).

1. Subir los cambios revisados al repositorio del proyecto.
2. En Atlas, crear un cluster y un usuario de base de datos con permisos sobre la
   base SIRA. Agregar a Network Access las IP/rangos de salida del servicio API.
   Obtener la URI de conexion con nombre de base. Guardarla solo en el panel de Render.
3. En Render, crear un Blueprint conectado al repositorio usando `render.yaml`.
   Revisar los planes antes de crear los recursos. El archivo propone servicios gratuitos.
4. Render conecta las variables PostgreSQL desde su base gestionada. La API usa
   JDBC con TLS y Flyway aplica V1-V3 durante el arranque; Hibernate valida el esquema.
5. En los campos pendientes del Blueprint, configurar la URI Atlas, una clave JWT
   aleatoria de produccion (32 bytes o mas en Base64), dos passwords propias de
   produccion y el dominio HTTPS de la SPA. No reutilizar la password demo.
6. Establecer el origen publico real de la API en la variable de build de la SPA.
   Copiar las URLs asignadas por Render, sin suponer que los nombres del YAML
   estaran disponibles como subdominios. Actualizar CORS con el origen real de la SPA.
7. Volver a desplegar los servicios despues de configurar sus dominios. Los cambios
   de la variable Vite requieren reconstruir la SPA. CORS requiere reiniciar la API.
8. Verificar `/actuator/health`, `/swagger-ui.html`, `/admin/login` y la SPA desde
   una ventana privada. Ejecutar el flujo anterior, comprobar ambos roles y repetir
   desde un telefono. Revisar que las respuestas de CORS incluyan solo el origen real.
9. Completar [entrega-lab6.md](entrega-lab6.md) con las URLs verificadas. Entregar
   las passwords al docente mediante el aula virtual, fuera del repositorio.

Las cuentas inicializadas en produccion son `mariana.vargas@sira.local`
(PROFESIONAL) y `laura.mendez@sira.local` (ENCARGADO), con las passwords privadas
configuradas en la plataforma. Las otras cuentas de ejemplo permanecen sin password.
PostgreSQL conserva los hashes BCrypt. El navegador nunca recibe estos hashes.

Los servicios deben seguir disponibles hasta la calificacion. Render documenta
que PostgreSQL gratuito expira a los 30 dias y que las API gratuitas se suspenden
por inactividad. Si el periodo de evaluacion excede la vigencia del plan, elegir
un plan o proveedor adecuado antes de la entrega, previa decision del propietario.

MongoDB almacena la bitacora documental consultada por la capa de datos; el cierre
transaccional y sus resultados se guardan en PostgreSQL, como en el laboratorio 5.
El health check incluye ambas bases: no desactivar sus indicadores para ocultar
una conexion fallida.

## Verificacion

```bash
./gradlew build
cd spa
npm ci
npm test
npm run build
```

La suite MVC local usa H2 y verifica login real, rechazo sin CSRF, restricciones
por rol/propiedad, aislamiento entre sesion y JWT, validacion, PRG y CORS. La suite
REST anterior sigue ejecutandose. Las pruebas PostgreSQL/Testcontainers requieren
Docker; CI rechaza que se omitan. Las pruebas React recorren login, carga/error,
reintento, paginacion, cierre, conservacion de respuestas ante un error y expiracion.

Si npm 10 falla con `edgesOut` al resolver Vitest, ejecutar `npx --yes npm@11.6.2 install`
para generar el lockfile. Con el lockfile versionado, usar `npm ci` en local y CI.
En Windows, si Java falla con `UnixDomainSockets.connect0`/loopback, usar un
directorio temporal absoluto valido mediante la propiedad `jdk.net.unixdomain.tmpdir`
solo en la terminal de esa ejecucion.

### Resultado local del 22 de octubre de 2026

- `gradlew build`: correcto; 98 pruebas descubiertas, 66 ejecutadas sin fallos y
  32 de integracion omitidas por no disponer de Docker. Cobertura de lineas en
  `business`: 98.4 %. La CI mantiene la exigencia de PostgreSQL real sin omisiones.
- `npm ci`, 7 pruebas React y build Vite: correctos. Auditoria de dependencias
  durante la instalacion: sin vulnerabilidades reportadas.
- `render.yaml`: valido contra el esquema JSON oficial de Render.
- Navegador: escritorio y movil sin desbordamiento horizontal. Prueba completa
  adicional contra el servidor Spring local y H2 temporal: crear participante,
  crear rutina de dos pasos, publicar desde MVC, ingresar como encargado en React,
  cerrar con un paso LOGRADO y otro CON_APOYO y consultar el 75 % en el historial.
- La prueba con H2 no acredita el despliegue ni sustituye las pruebas con
  PostgreSQL/MongoDB gestionados.
- La CI de GitHub aprobo tambien las pruebas reales PostgreSQL/Testcontainers:
  [ejecucion del commit de implementacion](https://github.com/cerdasanthony/sira-eif509/actions/runs/39842856495).
- La comprobacion publica posterior verifico health `UP`, acceso de ambos roles,
  Swagger UI y un cierre completo desde React con 87.5 % de adherencia guardado
  y consultado desde el historial. El backend usa PostgreSQL gestionado en Render
  y el cluster `sira` de Atlas; no usa H2 en produccion.

## Fuentes de configuracion

- [Spring Security: multiples cadenas](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
- [Vite: variables de entorno y build](https://vite.dev/guide/env-and-mode)
- [Render: especificacion de Blueprints](https://render.com/docs/blueprint-spec)
- [Render: limites del plan gratuito](https://render.com/docs/free)
- [Atlas: control de acceso por IP](https://www.mongodb.com/docs/atlas/security/ip-access-list/)

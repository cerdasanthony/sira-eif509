# Laboratorio 4 - Capa de negocio completa

## Procesos implementados

### Publicar una rutina

`PublicacionRutinaService` recibe `PublicarRutinaEntrada`, carga la rutina y el
profesional, ejecuta las especificaciones de negocio y devuelve
`PublicarRutinaSalida`. Las reglas verifican que el profesional este activo y
asignado, el participante este activo, la rutina siga en borrador, tenga al
menos dos pasos consecutivos, una vigencia valida, dias configurados y ninguna
franja traslapada con otra rutina publicada del participante. La duracion total
es la suma de `duracionEstimadaMin` de sus pasos.

### Cerrar la ejecucion diaria

`CierreEjecucionService` recibe `CerrarEjecucionEntrada` y realiza un caso de
uso transaccional completo:

1. Verifica la rutina publicada, el encargado asignado, la fecha, la vigencia,
   el dia, las horas, la ausencia de otro cierre y el conjunto exacto de pasos.
2. Crea y persiste la ejecucion abierta.
3. Persiste un `RegistroPaso` por cada paso.
4. Calcula la adherencia y cierra la ejecucion.

El metodo completo esta bajo `@Transactional`; por tanto, las escrituras se
confirman juntas o se revierten juntas. La formula aplicada es:

```text
adherencia = (LOGRADO * 1 + CON_APOYO * 0.5 + NO_LOGRADO * 0)
             / cantidad de pasos * 100
```

## Frontera de DTOs

Las entradas y salidas son `record` y viven en `business.dto`. Las entradas
usan Bean Validation, incluida la validacion en cascada de cada resultado. Los
controladores solo reciben y devuelven DTOs; ninguna entidad JPA cruza la
frontera de la capa de negocio. El mapeo es manual porque cada caso de uso tiene
pocos campos y el mapeo explicito hace visibles las decisiones del dominio.

## Patrones de diseno y justificacion

### Strategy - calculo de adherencia

**Senal:** la formula y los pesos de adherencia son una politica de negocio que
puede cambiar sin que cambie la orquestacion transaccional.

**Justificacion:** `CalculoAdherencia` encapsula la politica y
`CalculoAdherenciaPonderado` es la estrategia actual, por lo que otra formula se
puede sustituir sin modificar `CierreEjecucionService`.

### Specification - reglas de publicacion

**Senal:** publicar concentra varias reglas independientes que deben poder
probarse y evolucionar por separado.

**Justificacion:** cada `EspecificacionPublicacionRutina` encapsula una regla y
el servicio las compone, evitando un metodo monolitico lleno de condicionales.

Las especificaciones actuales son `ProfesionalAutorizadoParaPublicar`,
`RutinaCompletaParaPublicar` y `RutinaSinTraslape`.

## Rollback observable

`CierreEjecucionRollbackIntegracionTest` usa PostgreSQL 16 con Testcontainers e
instala un trigger exclusivo de la prueba. El trigger provoca un error al
insertar el segundo grupo de escrituras, despues de insertar la ejecucion. Al
salir del servicio se comprueba que no existe la ejecucion ni ningun registro de
paso de esa fecha, demostrando el rollback real de la transaccion.

## Pruebas y cobertura

La suite contiene pruebas unitarias con Mockito para caminos felices y errores
de ambos procesos, pruebas directas de Strategy y Specification, las pruebas de
persistencia del Laboratorio 3 y la prueba de rollback. JaCoCo genera los
reportes XML y HTML y hace fallar `check` si la cobertura de lineas de la capa
de negocio es inferior al 70 %.

```bash
./gradlew clean check
```

El reporte HTML queda en `build/reports/jacoco/test/html/index.html`.

## Endpoints

```text
POST /api/rutinas/publicaciones
POST /api/ejecuciones/cierres
```

Los errores de formato producen HTTP 400, los recursos inexistentes HTTP 404 y
las violaciones de reglas de negocio HTTP 422.

## Base de datos

Este laboratorio no requirio cambios de esquema. Las tablas y restricciones de
las migraciones `V1` y `V2` ya soportaban ambos procesos, por lo que no se creo
una migracion Flyway innecesaria.

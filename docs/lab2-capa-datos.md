# Laboratorio 2 - Capa de datos completa

## Decision general

SIRA usa persistencia poliglota:

- PostgreSQL guarda el nucleo transaccional: usuarios, participantes, rutinas, pasos, ejecuciones y resultados por paso.
- MongoDB guarda la bitacora de observaciones asociada a una ejecucion.

La separacion mantiene consistencia fuerte donde hay reglas de negocio y deja en documentos las notas libres que cambian segun el contexto.

## Modelo PostgreSQL

El esquema esta en `src/main/resources/db/migration/V1__crear_esquema_sira.sql`.

Tablas principales:

- `usuario`: profesionales y encargados. Tiene `rol`, correo unico y validacion de formato.
- `participante`: persona que sigue rutinas. Referencia a un profesional y a un encargado.
- `rutina`: plantilla asignada a un participante, con estado y vigencia.
- `rutina_dia_semana`: normaliza los dias aplicables de una rutina.
- `paso_rutina`: pasos ordenados de cada rutina.
- `ejecucion`: corrida diaria de una rutina.
- `registro_paso`: resultado de cada paso dentro de una ejecucion.

El modelo queda en 3FN porque cada tabla representa una sola entidad o relacion, los atributos dependen de la llave completa y no se guardan grupos repetidos dentro de una columna. Por ejemplo, los dias de la semana no se guardan como texto separado por comas en `rutina`; estan en `rutina_dia_semana`.

## Restricciones

Restricciones relevantes:

- `UNIQUE` en `usuario.correo` para evitar cuentas duplicadas.
- `UNIQUE` en `(rutina_id, orden)` para que una rutina no tenga dos pasos con el mismo orden.
- `UNIQUE` en `(rutina_id, fecha)` para impedir cerrar dos veces la misma rutina el mismo dia.
- `CHECK` para roles, estados, resultados, fechas de vigencia, duracion de pasos y rango de adherencia.
- `FK` para proteger relaciones entre usuarios, participantes, rutinas, ejecuciones y pasos.

## Indices

Los indices se eligieron segun las consultas esperadas:

- `idx_participante_profesional`: listar participantes atendidos por un profesional.
- `idx_participante_encargado`: listar participantes visibles para un encargado.
- `idx_rutina_participante_estado`: consultar rutinas activas/publicadas de un participante.
- `idx_rutina_vigencia`: filtrar rutinas por rango de vigencia.
- `idx_paso_rutina_rutina_orden`: cargar pasos de una rutina en orden.
- `idx_ejecucion_rutina_fecha`: consultar adherencia historica por rutina y fecha.
- `idx_ejecucion_registrado_por`: auditar ejecuciones registradas por un usuario.
- `idx_registro_paso_paso`: revisar el comportamiento historico de un paso especifico.

## Migraciones y seeds

Flyway reconstruye la base desde cero con:

- `V1__crear_esquema_sira.sql`: crea tablas, restricciones, indices y comentarios.
- `V2__datos_ejemplo.sql`: inserta usuarios, participantes, rutinas, pasos, ejecuciones cerradas y registros de pasos.

Los datos de ejemplo representan un centro pequeno de terapia y apoyo educativo en Alajuela, con profesionales, encargados y rutinas reales del dominio.

## Subdominio MongoDB

La coleccion `bitacora_observaciones` esta en `docker/mongo/init/01_bitacora_observaciones.js`.

Se eligio MongoDB para la bitacora porque las observaciones son notas libres con estructura variable. Algunas tienen etiquetas simples, otras guardan contexto del ambiente, sugerencias profesionales, cambios detectados o datos de apoyo usado. En PostgreSQL eso produciria columnas opcionales o varias tablas auxiliares para datos que casi siempre se leen juntos.

### Preguntas de diseno

**Como se lee este dato el 90% del tiempo?**

La observacion se lee completa: nota, etiquetas, autor y contexto. Por eso conviene incrustar esos datos dentro del documento.

**Cuanto crece en el peor caso?**

La bitacora puede crecer sin limite por participante. Por eso no se incrusta dentro de `participante` ni de `ejecucion`; cada observacion es un documento independiente y referencia los IDs relacionales.

**Quien mas lo necesita?**

Las ejecuciones, participantes, rutinas y usuarios son compartidos por otros procesos y siguen en PostgreSQL. MongoDB guarda una copia pequena del autor para lectura rapida, pero mantiene `usuarioId`, `participanteId`, `rutinaId` y `ejecucionId` como referencias.

## Docker Compose

El archivo `docker-compose.yml` levanta:

- PostgreSQL 16 en el puerto `5432`.
- MongoDB 7 en el puerto `27017`.
- Flyway 10 para aplicar las migraciones sobre PostgreSQL.

Comando:

```bash
docker compose up -d
```

Para reiniciar desde cero durante pruebas:

```bash
docker compose down -v
docker compose up -d
```

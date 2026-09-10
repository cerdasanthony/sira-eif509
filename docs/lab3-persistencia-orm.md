# Laboratorio 3 - Persistencia con ORM y repositorios

## Alcance implementado

La capa de persistencia usa PostgreSQL para el nucleo transaccional y MongoDB para
la bitacora de observaciones. Flyway conserva la responsabilidad de crear el
esquema; Hibernate se ejecuta con `ddl-auto=validate`, por lo que no modifica la
base y detiene el arranque si el mapeo no coincide con las migraciones V1 y V2.

Se mapearon las seis entidades relacionales del dominio:

| Entidad JPA | Tabla | Relaciones principales |
|---|---|---|
| `Usuario` | `usuario` | participantes atendidos, participantes a cargo y ejecuciones registradas |
| `Participante` | `participante` | profesional, encargado y rutinas |
| `Rutina` | `rutina` | participante, dias de semana, pasos y ejecuciones |
| `PasoRutina` | `paso_rutina` | rutina y registros del paso |
| `Ejecucion` | `ejecucion` | rutina, usuario que registra y registros de pasos |
| `RegistroPaso` | `registro_paso` | ejecucion y paso evaluado |

Todas las asociaciones se declaran `LAZY`, incluso las relaciones `ManyToOne`,
que en JPA serian `EAGER` por defecto. Los enums se persisten por nombre con
`EnumType.STRING`, en concordancia con los `CHECK` del esquema.

`BitacoraObservacion` representa la coleccion `bitacora_observaciones` de
MongoDB. Conserva el autor embebido, las referencias a los IDs relacionales, las
etiquetas y el contexto de estructura variable.

## Repositorios generalizados

La jerarquia evita repetir contratos CRUD:

```text
RepositorioJpaBase<T, ID>
  -> JpaRepository<T, ID>
  -> JpaSpecificationExecutor<T>
  -> UsuarioRepository, ParticipanteRepository, RutinaRepository,
     PasoRutinaRepository, EjecucionRepository y RegistroPasoRepository

RepositorioMongoBase<T, ID>
  -> MongoRepository<T, ID>
  -> BitacoraObservacionRepository
```

Los repositorios base tienen `@NoRepositoryBean`; Spring solo crea los
repositorios concretos.

## Consultas de negocio

### JPQL 1 - rutinas publicadas de un participante

`RutinaRepository.buscarPublicadasDelParticipante(participanteId)` selecciona
las rutinas publicadas y las ordena por hora de inicio.

SQL equivalente generado por Hibernate (columnas abreviadas):

```sql
select r.*
from rutina r
where r.participante_id = ?
  and r.estado = 'PUBLICADA'
order by r.hora_inicio
```

### JPQL 2 - historial cerrado de una rutina

`EjecucionRepository.buscarCerradasDeRutinaEnRango(rutinaId, desde, hasta)`
recupera las ejecuciones cerradas del periodo en orden cronologico.

```sql
select e.*
from ejecucion e
where e.rutina_id = ?
  and e.estado = 'CERRADA'
  and e.fecha between ? and ?
order by e.fecha
```

### Criteria 1 - busqueda dinamica de rutinas

`RutinaSpecifications.conFiltros(...)` combina solo los parametros presentes:
participante, estado y una fecha en la que la rutina debe estar vigente. La
vigencia trata correctamente el `vigencia_hasta IS NULL` como rango abierto.

SQL generado para los tres filtros presentes (Hibernate omite del `WHERE` los
predicados cuyos argumentos llegan en `null`):

```sql
select r.*
from rutina r
where r.participante_id = ?
  and r.estado = ?
  and r.vigencia_desde <= ?
  and (r.vigencia_hasta is null or r.vigencia_hasta >= ?)
```

### Criteria 2 - busqueda dinamica de ejecuciones

`EjecucionSpecifications.conFiltros(...)` combina rutina, estado, fecha inicial,
fecha final y adherencia minima. Los cinco filtros son opcionales y se traducen
en predicados de Criteria, no en concatenacion de SQL.

SQL generado cuando se proporcionan todos los filtros:

```sql
select e.*
from ejecucion e
where e.rutina_id = ?
  and e.estado = ?
  and e.fecha >= ?
  and e.fecha <= ?
  and e.adherencia >= ?
```

## Evidencia y correccion del problema N+1

El caso real es el listado de rutinas publicadas con sus pasos. La consulta
`buscarPublicadasSinPasos()` carga tres rutinas y, al recorrer `getPasos()`,
Hibernate ejecuta una consulta adicional por rutina:

```sql
-- Consulta inicial (1)
select r.* from rutina r where r.estado = 'PUBLICADA' order by r.id;

-- Consulta de la coleccion (N = 3)
select p.* from paso_rutina p where p.rutina_id = ? order by p.orden;
select p.* from paso_rutina p where p.rutina_id = ? order by p.orden;
select p.* from paso_rutina p where p.rutina_id = ? order by p.orden;
```

Resultado antes: **4 sentencias preparadas (1 + N)**.

La correccion es `buscarPublicadasConPasos()`, que aplica
`@EntityGraph(attributePaths = "pasos")`. Hibernate resuelve el grafo mediante
un `LEFT JOIN` en una sola sentencia:

```sql
select distinct r.*, p.*
from rutina r
left join paso_rutina p on p.rutina_id = r.id
where r.estado = 'PUBLICADA'
order by r.id, p.orden;
```

Resultado despues: **1 sentencia preparada**. La prueba
`entityGraphCorrigeElNMasUnoAlCargarPasos` habilita las estadisticas de Hibernate,
reproduce ambos caminos y verifica automaticamente los conteos 4 y 1.

## Pruebas de integracion

`PersistenciaIntegracionTest` contiene ocho pruebas contra un contenedor real de
PostgreSQL 16:

1. Flyway reconstruye el esquema y carga todos los datos V2.
2. Las relaciones de `Participante` permanecen `LAZY`.
3. El repositorio generico ofrece CRUD real.
4. Primera consulta JPQL.
5. Segunda consulta JPQL.
6. Primera consulta Criteria/Specification.
7. Segunda consulta Criteria/Specification.
8. Evidencia automatica y correccion del N+1.

Ejecucion local:

```bash
./gradlew test
```

Testcontainers crea y elimina PostgreSQL automaticamente; no usa H2 ni depende
de la base levantada por `docker compose`. En una maquina sin Docker, la clase de
integracion se omite y las pruebas unitarias siguen disponibles. GitHub Actions
dispone de Docker y ejecuta la suite de integracion en cada `push` o pull request.

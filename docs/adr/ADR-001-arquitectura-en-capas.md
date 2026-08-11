# ADR-001 · Organización del código en capas técnicas

**Estado:** Aceptada · **Fecha:** 10/08/2026 · **Responsable:** Anthony Cerdas Chacón

---

## Contexto

SIRA es un sistema de gestión de rutinas de apoyo que se va a construir de forma **incremental a lo
largo de siete laboratorios**: en el Lab 1 solo el esqueleto, en el Lab 2 entra PostgreSQL y MongoDB,
en el Lab 3 la API REST completa, en el Lab 4 una operación transaccional (el cierre de la ejecución
diaria), en el Lab 5 seguridad por roles y en el Lab 6 un frontend React separado.

Las restricciones son concretas:

- El proyecto lo desarrolla **una sola persona**, en paralelo con el resto del ciclo.
- El curso exige explícitamente separación en presentación, lógica de negocio y acceso a datos, y la
  rúbrica del Lab 1 evalúa esa separación con 3.0 de los 10 puntos.
- El dominio tiene una operación que **debe ser transaccional** (crear la `Ejecucion` + N
  `RegistroPaso` + calcular la adherencia, todo o nada). Haga lo que haga la estructura de paquetes,
  tiene que existir un lugar obvio donde poner el límite de esa transacción.
- El sistema es pequeño: 6 entidades y 2 procesos de negocio.

Hay que decidir **cómo se organizan los paquetes** antes de escribir el primer controlador, porque
mover paquetes después es barato en el Lab 1 y caro en el Lab 5.

## Decisión

**Organizamos el código en capas técnicas**, con un paquete por capa bajo `cr.ac.una.sira`:

```
presentation/   controladores REST y DTOs de entrada/salida
business/       servicios: reglas, validaciones, cálculos y límites de transacción
data/           repositorios y modelo persistente
config/         configuración transversal de Spring (no es una capa del flujo)
```

Las dependencias fluyen en **una sola dirección**: `presentation → business → data`. Ninguna capa
importa a la que tiene encima. Concretamente: `data/` no importa nada de `business/` ni de
`presentation/`, `business/` no importa nada de `presentation/`, y ningún tipo de
`org.springframework.web` aparece fuera de `presentation/`.

El límite transaccional del Lab 4 vivirá en `business/`, en el método del servicio que cierra la
ejecución diaria — no en el controlador ni en el repositorio.

## Alternativas consideradas

### 1 · Organización por funcionalidad (vertical slices / package-by-feature)

Un paquete por concepto de negocio (`rutina/`, `ejecucion/`, `participante/`), cada uno con su
controlador, servicio y repositorio adentro.

**Por qué se descartó:** es una organización mejor para sistemas grandes con equipos separados por
módulo, porque mantiene junto lo que cambia junto. Aquí no aplica: el sistema tiene 6 entidades y un
solo desarrollador, así que no hay equipos que aislar. Y tiene un costo concreto en este curso: la
rúbrica del Lab 1 y el diagrama de arquitectura piden mostrar las capas y su interacción; con
vertical slices la separación por capas existe pero queda difuminada dentro de cada módulo, y habría
que demostrarla archivo por archivo en lugar de carpeta por carpeta. Se descarta por costo de
demostración sin beneficio proporcional al tamaño del sistema.

### 2 · Arquitectura hexagonal (puertos y adaptadores)

El dominio en el centro, sin dependencias de framework, y adaptadores de entrada (HTTP) y de salida
(JPA, Mongo) conectados mediante interfaces definidas por el dominio.

**Por qué se descartó:** técnicamente es la que mejor protege el dominio, y es tentadora porque SIRA
va a usar **dos** motores de persistencia (PostgreSQL para lo relacional y MongoDB para la bitácora),
que es justo el escenario donde los puertos de salida lucen. Pero el costo es real: obliga a definir
una interfaz de puerto y al menos un adaptador por cada operación de datos, más objetos de dominio
separados de las entidades JPA con su mapeo correspondiente. Para 6 entidades eso es aproximadamente
el doble de archivos y de mapeo manual, mantenido por una sola persona con un calendario de entregas
semanales. Se descarta por complejidad desproporcionada al tamaño del problema, no por falta de
mérito técnico.

### 3 · Sin capas (controladores con la lógica adentro)

**Por qué se descartó:** incumple directamente el requisito del curso y hace imposible probar una
regla de negocio sin levantar el contexto web. La prueba `ParticipanteServiceTest` de este
laboratorio se ejecuta sin arrancar Spring precisamente porque el servicio recibe su repositorio por
constructor; con la lógica dentro del controlador esa prueba tendría que ser una prueba de
integración, más lenta y más frágil.

## Consecuencias

**Positivas**

- La regla de dependencias es verificable con una búsqueda de `import`, no depende de criterio: se
  puede revisar en cada laboratorio.
- Cambiar la fuente de datos no toca las otras capas. En el Lab 2, `ParticipanteRepository` pasa de
  una lista en memoria a Spring Data JPA sobre PostgreSQL sin modificar `ParticipanteService` ni
  `ParticipanteController`.
- Hay un único lugar obvio donde poner `@Transactional` en el Lab 4: el servicio.
- Las reglas de negocio se prueban con JUnit puro, sin levantar el contexto de Spring — pruebas de
  milisegundos, que la CI corre en cada push.

**Negativas**

- Un cambio de una sola funcionalidad toca tres paquetes distintos (agregar un campo a `Rutina`
  implica editar `data/`, `business/` y `presentation/`). Con package-by-feature sería una sola
  carpeta.
- Los paquetes `business/` y `data/` van a crecer hasta ~12 clases cada uno cuando estén las 6
  entidades. A ese tamaño todavía es manejable, pero no escala mucho más allá; si el sistema
  creciera, habría que subdividir por subdominio dentro de cada capa.
- El DTO de presentación (`ParticipanteResponse`) obliga a un mapeo manual que hoy, con dos campos,
  parece burocracia pura. Se asume ese costo a cambio de que el contrato HTTP no quede amarrado al
  modelo de datos.

**Neutras**

- Toda clase nueva exige decidir primero a qué capa pertenece. El criterio acordado: si la respuesta
  cambia según *quién* consume el sistema, va en presentación; si cambia según *cómo* se guardan los
  datos, va en datos; si es una decisión del negocio que sería igual con otra base de datos y otra
  interfaz, va en negocio.

## Referencias

- Guía de configuración del ambiente, EIF509 — Paso 5, «Organizar el proyecto por capas» y la regla
  de oro de dirección de dependencias.
- Esqueleto de referencia del curso (`eif509-esqueleto`), aula virtual.
- Documentación oficial de Spring Boot 3 — inyección por constructor y `@Transactional` a nivel de
  servicio.
- Sesión 2 del curso: arquitectura de software, capas y niveles.

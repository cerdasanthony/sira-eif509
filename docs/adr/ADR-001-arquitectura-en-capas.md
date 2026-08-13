# ADR-001: organizar el código en capas técnicas

**Estado:** Aceptada · **Fecha:** 10/08/2026 · **Responsable:** Anthony Cerdas Chacón

## Contexto

SIRA se va a construir de forma incremental durante siete laboratorios. En el Lab 1 solo el esqueleto; en el 2 entran PostgreSQL y MongoDB; en el 3 la API REST completa; en el 4 el cierre de la ejecución diaria como transacción; en el 5 seguridad por roles; en el 6 un frontend React separado.

Las restricciones que tengo son concretas. Desarrollo solo, en paralelo con el resto del ciclo. El curso pide separación entre presentación, lógica de negocio y acceso a datos, y esa separación vale 3.0 de los 10 puntos del laboratorio. El dominio tiene una operación que obligatoriamente debe ser transaccional, así que necesito un lugar evidente donde poner el límite de esa transacción. Y el sistema es chico: seis entidades y dos procesos.

Tengo que decidir cómo organizo los paquetes antes de escribir el primer controlador, porque mover paquetes ahora es barato y en el Lab 5 no lo va a ser.

## Decisión

Organizo el código en capas técnicas, con un paquete por capa bajo `cr.ac.una.sira`:

```
presentation/   controladores REST y DTOs
business/       servicios: reglas, validaciones, cálculos y límites de transacción
data/           repositorios y modelo persistente
config/         configuración transversal de Spring
```

Las dependencias van en una sola dirección: `presentation -> business -> data`. Ninguna capa importa a la que tiene encima. En concreto, `data/` no importa nada de `business/` ni de `presentation/`, `business/` no importa nada de `presentation/`, y ningún tipo de `org.springframework.web` aparece fuera de `presentation/`.

El límite transaccional del Lab 4 va a vivir en `business/`, en el método del servicio que cierra la ejecución diaria. No en el controlador y no en el repositorio.

## Alternativas consideradas

### Organización por funcionalidad (vertical slices)

Sería un paquete por concepto de negocio (`rutina/`, `ejecucion/`, `participante/`), cada uno con su controlador, servicio y repositorio adentro.

Es una organización mejor para sistemas grandes con equipos separados por módulo, porque mantiene junto lo que cambia junto. Acá no aplica: seis entidades y un solo desarrollador, así que no hay equipos que aislar. Y tiene un costo concreto en este curso, que es que la rúbrica del Lab 1 y el diagrama piden mostrar las capas y su interacción. Con vertical slices la separación existe, pero queda difuminada dentro de cada módulo y tendría que demostrarla archivo por archivo en vez de carpeta por carpeta. La descarté por eso, no porque sea peor en general.

### Arquitectura hexagonal (puertos y adaptadores)

El dominio en el centro sin dependencias de framework, con adaptadores de entrada (HTTP) y de salida (JPA, Mongo) conectados por interfaces que define el propio dominio.

Es la que mejor protege el dominio y estuve tentado de usarla, porque SIRA va a tener dos motores de persistencia distintos y ese es exactamente el escenario donde los puertos de salida se lucen. Lo que me frenó fue el costo: obliga a definir una interfaz de puerto y al menos un adaptador por cada operación de datos, más objetos de dominio separados de las entidades JPA con su mapeo correspondiente. Para seis entidades eso es más o menos el doble de archivos y de mapeo manual, mantenido por una persona con entregas semanales. La descarté por complejidad desproporcionada al tamaño del problema, no por falta de mérito técnico.

### Sin capas, con la lógica dentro de los controladores

Incumple directamente lo que pide el curso y haría imposible probar una regla de negocio sin levantar el contexto web. La prueba `ParticipanteServiceTest` de este laboratorio corre sin arrancar Spring justamente porque el servicio recibe su repositorio por constructor; con la lógica dentro del controlador tendría que ser una prueba de integración, más lenta y más frágil.

## Consecuencias

### Positivas

La regla de dependencias se puede verificar con una búsqueda de `import`, así que no depende del criterio de nadie y la puedo revisar en cada laboratorio.

Cambiar la fuente de datos no toca las otras capas. En el Lab 2, `ParticipanteRepository` pasa de una lista en memoria a Spring Data JPA sobre PostgreSQL sin que `ParticipanteService` ni `ParticipanteController` cambien.

Hay un único lugar obvio donde poner `@Transactional` en el Lab 4.

Las reglas de negocio se prueban con JUnit puro, sin levantar el contexto de Spring. Son pruebas de milisegundos, que la CI corre en cada push.

### Negativas

Un cambio de una sola funcionalidad toca tres paquetes distintos. Agregarle un campo a `Rutina` implica editar `data/`, `business/` y `presentation/`; con package-by-feature sería una sola carpeta.

Los paquetes `business/` y `data/` van a crecer hasta unas 12 clases cada uno cuando estén las seis entidades. A ese tamaño todavía se maneja, pero no escala mucho más allá. Si el sistema creciera, habría que subdividir por subdominio dentro de cada capa.

El DTO de presentación (`ParticipanteResponse`) obliga a un mapeo manual que hoy, con dos campos, parece burocracia pura. Asumo ese costo a cambio de que el contrato HTTP no quede amarrado al modelo de datos.

### Neutras

Cada clase nueva me obliga a decidir primero a qué capa pertenece. El criterio que estoy usando: si la respuesta cambiaría según quién consume el sistema, va en presentación; si cambiaría según cómo se guardan los datos, va en datos; si sería igual con otra base de datos y otra interfaz, va en negocio.

## Referencias

- Guía de configuración del ambiente, EIF509, paso 5 sobre organizar el proyecto por capas.
- Esqueleto de referencia del curso (`eif509-esqueleto`), aula virtual.
- Documentación de Spring Boot 3 sobre inyección por constructor y `@Transactional` a nivel de servicio.
- Sesión 2 del curso: arquitectura de software, capas y niveles.

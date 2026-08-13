# ADR-001: organizar el código en capas técnicas

**Estado:** Aceptada · **Fecha:** 10/08/2026 · **Responsable:** Anthony Cerdas Chacón

## Contexto

SIRA se construye de forma incremental durante siete laboratorios: en el 2 entran PostgreSQL y MongoDB, en el 3 la API REST, en el 4 una operación transaccional, en el 5 seguridad por roles y en el 6 un frontend React aparte. Desarrollo solo y el sistema es chico, seis entidades y dos procesos. El curso pide separar presentación, lógica de negocio y acceso a datos.

Tengo que decidir cómo organizo los paquetes antes de escribir el primer controlador, porque moverlos ahora es barato y más adelante no.

## Decisión

Organizo el código en capas técnicas, un paquete por capa bajo `cr.ac.una.sira`: `presentation/` para controladores y DTOs, `business/` para servicios, `data/` para repositorios y modelo, y `config/` para configuración de Spring.

Las dependencias van en una sola dirección: `presentation -> business -> data`. El límite transaccional del Lab 4 va a estar en `business/`, en el servicio que cierra la ejecución diaria.

## Alternativas consideradas

**Organización por funcionalidad (vertical slices).** Un paquete por concepto de negocio (`rutina/`, `ejecucion/`), cada uno con su controlador, servicio y repositorio adentro. Es mejor para sistemas grandes con equipos separados por módulo, pero acá hay seis entidades y una sola persona, así que no hay equipos que aislar. Además la separación por capas queda difuminada dentro de cada módulo, y el laboratorio pide un diagrama que las muestre.

**Arquitectura hexagonal.** El dominio en el centro con adaptadores de entrada y salida conectados por interfaces. Es la que mejor protege el dominio y me llamó la atención porque SIRA va a usar dos motores de persistencia distintos. Lo que me frenó es que obliga a una interfaz y un adaptador por cada operación de datos, más objetos de dominio separados de las entidades JPA. Para seis entidades es más o menos el doble de archivos, y lo mantengo yo solo con entregas semanales.

## Consecuencias

**Positivas.** La regla de dependencias se verifica buscando los imports, no depende del criterio de nadie. Cambiar la fuente de datos no toca las otras capas: en el Lab 2, `ParticipanteRepository` pasa a Spring Data JPA sin que el servicio ni el controlador cambien. Y las reglas se prueban con JUnit sin levantar Spring, que es lo que hace `ParticipanteServiceTest`.

**Negativas.** Un cambio de una funcionalidad toca tres paquetes: agregarle un campo a `Rutina` implica editar `data/`, `business/` y `presentation/`. Los paquetes `business/` y `data/` van a llegar a unas doce clases cada uno con las seis entidades, que todavía se maneja pero no escala mucho más. Y el DTO obliga a un mapeo manual que hoy, con dos campos, parece burocracia.

## Referencias

- Guía de configuración del ambiente del curso, paso 5.
- Esqueleto de referencia del curso (`eif509-esqueleto`), aula virtual.
- Documentación de Spring Boot 3.

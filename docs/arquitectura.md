# Arquitectura de SIRA

El sistema está organizado en tres capas sobre Spring Boot 3. El porqué de esta organización está en el [ADR-001](adr/ADR-001-arquitectura-en-capas.md).

## Las capas

```mermaid
flowchart TB
    NAV["Navegador / curl"]

    subgraph app["cr.ac.una.sira"]
        direction TB

        subgraph pres["presentation/"]
            SC["SaludController"]
            PC["ParticipanteController"]
            DTO["ParticipanteResponse"]
        end

        subgraph biz["business/"]
            SS["SaludService"]
            PS["ParticipanteService"]
        end

        subgraph dat["data/"]
            SR["SaludRepository"]
            PR["ParticipanteRepository"]
            JPA["6 entidades JPA"]
            REPOS["Repositorios JPA genericos"]
            MONGO["Repositorio de bitacora"]
        end

        CFG["config/"]
    end

    PG[("PostgreSQL<br/>desde el Lab 2")]
    MG[("MongoDB<br/>desde el Lab 2")]

    NAV -->|HTTP / JSON| SC
    NAV -->|HTTP / JSON| PC
    PC -.->|construye| DTO
    SC --> SS
    PC --> PS
    SS --> SR
    PS --> PR
    PR -->|devuelve| JPA
    REPOS --> PG
    MONGO --> MG
```

Las flechas representan dependencias reales del Laboratorio 3.

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Presentación | `presentation/` | Recibir HTTP y devolver JSON. Sin reglas de negocio. |
| Negocio | `business/` | Reglas, validaciones, cálculos y límites de transacción. |
| Datos | `data/` | Leer y guardar en la fuente de datos. |
| Configuración | `config/` | Beans de Spring. No es una capa del flujo, es transversal. |

## La regla de las dependencias

```
presentation  ->  business  ->  data
```

En una sola dirección. Un controlador puede llamar a un servicio, pero un servicio nunca debe saber que existe HTTP. Se puede comprobar buscando los imports: en `data/` no aparece nada de `business` ni de `presentation`, en `business/` no aparece nada de `presentation`, y `org.springframework.web` no aparece fuera de `presentation/`.

## Cómo interactúan

`GET /api/participantes`:

```mermaid
sequenceDiagram
    actor U as Navegador
    participant C as ParticipanteController
    participant S as ParticipanteService
    participant R as ParticipanteRepository
    participant PG as PostgreSQL

    U->>C: GET /api/participantes
    C->>S: listarActivos()
    S->>R: findByActivoTrueOrderByNombreAsc()
    R->>PG: SELECT activos ORDER BY nombre
    PG-->>R: participantes activos
    R-->>S: participantes ordenados
    S-->>C: los 2 activos
    C-->>U: 200 OK · JSON
```

El servicio expresa el caso de uso y el repositorio hace el filtrado y ordenamiento
en PostgreSQL, evitando cargar filas que la API no necesita.

## Estructura

```
src/main/java/cr/ac/una/sira/
├── SiraApplication.java
├── presentation/   SaludController, ParticipanteController, ParticipanteResponse
├── business/       SaludService, ParticipanteService
├── data/           Entidades JPA, documento Mongo, repositorios y Specifications
└── config/         Configuración transversal

src/test/java/cr/ac/una/sira/
├── SiraApplicationTests.java
├── business/ParticipanteServiceTest.java
└── data/PersistenciaIntegracionTest.java
```

La evidencia de JPQL, Criteria y del problema N+1 esta en
[`lab3-persistencia-orm.md`](lab3-persistencia-orm.md).

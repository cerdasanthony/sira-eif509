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
            MOD["Participante"]
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
    PR -.->|devuelve| MOD
    PR -.-> PG
    PR -.-> MG
```

Las flechas continuas son dependencias reales en el código. Las punteadas todavía no existen.

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

    U->>C: GET /api/participantes
    C->>S: listarActivos()
    S->>R: buscarTodos()
    R-->>S: los 3 participantes
    Note over S: filtra inactivos y ordena
    S-->>C: los 2 activos
    C-->>U: 200 OK · JSON
```

El repositorio devuelve tres y la API responde dos. El filtro está en el servicio y no en el repositorio porque "solo se asignan rutinas a participantes activos" es una regla de negocio, no un detalle de almacenamiento.

## Estructura

```
src/main/java/cr/ac/una/sira/
├── SiraApplication.java
├── presentation/   SaludController, ParticipanteController, ParticipanteResponse
├── business/       SaludService, ParticipanteService
├── data/           SaludRepository, ParticipanteRepository, Participante
└── config/         (vacío en el Lab 1)

src/test/java/cr/ac/una/sira/
├── SiraApplicationTests.java
└── business/ParticipanteServiceTest.java
```

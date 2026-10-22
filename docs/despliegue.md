# Diagrama de despliegue - Laboratorio 6

Topologia preparada en `render.yaml`. Los servicios y dominios reales estan
pendientes de aprovisionamiento; registrar sus URLs verificadas en
[entrega-lab6.md](entrega-lab6.md).

```mermaid
flowchart TB
    familia["Navegador del encargado / telefono"]
    profesional["Navegador del profesional"]
    git["GitHub: sira-eif509"]
    ci["GitHub Actions: Gradle + React"]
    subgraph render["Render - HTTPS gestionado"]
      spa["Sitio estatico React / Vite\nVITE_API_URL en build"]
      api["Servicio Docker Java 21 / Spring Boot\nPerfil prod, PORT\nSIRA_CORS_ORIGINS\nSIRA_JWT_SECRET\nPasswords iniciales por rol"]
      mvc["MVC Thymeleaf /admin\nSesion + CSRF"]
      rest["REST /api/v1 + /auth/login\nJWT + roles + propiedad\nSwagger UI /swagger-ui.html"]
      biz["Servicios compartidos de negocio"]
      pg[("PostgreSQL 16 gestionado\nMigraciones Flyway V1-V3")]
    end
    subgraph atlas["MongoDB Atlas"]
      mongo[("MongoDB gestionado\nBitacora documental")]
    end
    git --> ci
    git -->|"Dockerfile"| api
    git -->|"npm ci + build"| spa
    familia -->|"HTTPS"| spa
    familia -->|"HTTPS + Bearer; origen CORS exacto"| rest
    profesional -->|"HTTPS + cookie Secure/HttpOnly"| mvc
    api --- mvc
    api --- rest
    mvc --> biz
    rest --> biz
    biz -->|"JDBC TLS; SIRA_POSTGRES_*"| pg
    biz -->|"TLS; SIRA_MONGODB_URI; IP autorizada"| mongo
```

| Ubicacion | Configuracion |
|---|---|
| Render, servicio API | Perfil, dominio SPA autorizado, clave JWT y passwords iniciales |
| Render, conexiones de PostgreSQL | Host, puerto, base, usuario y password; inyectados al servicio API |
| Render, build SPA | Origen HTTPS publico de la API; dato publico incorporado al bundle |
| Atlas | Usuario de base, permisos, TLS e IP/rangos de salida de la API permitidos |
| GitHub | Codigo, migraciones, definicion de infraestructura y CI; sin secretos de produccion |

La lista completa de nombres de variables esta en el README. API y MVC comparten
contenedor y origen. PostgreSQL y MongoDB se conectan por protocolos propios con
TLS; las interfaces web y Swagger se publican mediante HTTPS. Las bases no se
exponen como endpoints HTTP para el navegador.

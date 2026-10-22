# Entrega del Laboratorio 6

Repositorio: https://github.com/cerdasanthony/sira-eif509

Estado actual: desplegado y verificado el 22 de octubre de 2026.

| Dato requerido | Resultado verificado |
|---|---|
| URL API HTTPS | https://sira-api-wzo1.onrender.com |
| URL SPA HTTPS | https://sira-spa.onrender.com |
| URL Swagger UI | https://sira-api-wzo1.onrender.com/swagger-ui.html |
| URL modulo MVC | https://sira-api-wzo1.onrender.com/admin/login |
| Usuario PROFESIONAL | `mariana.vargas@sira.local` |
| Usuario ENCARGADO | `laura.mendez@sira.local` |
| Password por rol | Compartir en el aula virtual; nunca versionar |

Antes de entregar, confirmar acceso con ambos roles y un cierre completo desde
la SPA. Mantener los servicios disponibles hasta la publicacion de la calificacion.
Render informa que PostgreSQL gratuito vence el 21 de noviembre de 2026; revisar
la vigencia si la calificacion se publica despues de esa fecha.

Las passwords de prueba estan en las variables privadas `SIRA_PROFESIONAL_PASSWORD`
y `SIRA_ENCARGADO_PASSWORD` del servicio API de Render. No se guardan en Git.

## Verificacion publica realizada

- `/actuator/health` devuelve `UP`, con PostgreSQL y MongoDB Atlas conectados.
- Inicio de sesion del PROFESIONAL en MVC y listado de sus participantes.
- Inicio de sesion JWT del ENCARGADO en la SPA, con CORS para su origen real.
- Cierre de la rutina de demostracion 1 el 22 de octubre, de 06:30 a 06:50,
  con cuatro resultados. La API calculo 87.5 % y una consulta nueva del historial
  mostro el registro CERRADA, confirmando su persistencia.
- Swagger UI carga el contrato SIRA API REST v1 y sus operaciones.

El despliegue utiliza el commit `baf1c95`; los cambios posteriores de esta
entrega actualizan la documentacion. Se uso la URL publica del repositorio;
si no se conecta una integracion Git, los despliegues posteriores se solicitan
desde el panel de Render.

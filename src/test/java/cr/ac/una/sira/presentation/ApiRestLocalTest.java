package cr.ac.una.sira.presentation;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

/** Misma suite de contrato para desarrollo sin Docker. PostgreSQL se verifica en la CI. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.data.mongodb.repositories.enabled=false",
        "management.health.mongo.enabled=false",
        "sira.demo.enabled=false"
})
@Sql("/api-datos-locales.sql")
class ApiRestLocalTest extends ApiRestContrato { }

package cr.ac.una.sira.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TiempoConfig {

    @Bean
    Clock relojDelSistema() {
        return Clock.systemDefaultZone();
    }
}

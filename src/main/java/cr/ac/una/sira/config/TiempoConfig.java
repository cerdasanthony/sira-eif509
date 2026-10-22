package cr.ac.una.sira.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;

@Configuration
public class TiempoConfig {

    @Bean
    Clock relojDelSistema(@Value("${sira.time-zone}") String zona) {
        return Clock.system(java.time.ZoneId.of(zona));
    }
}

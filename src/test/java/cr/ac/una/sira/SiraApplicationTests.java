package cr.ac.una.sira;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class SiraApplicationTests {

	@Test
	void declaraLaConfiguracionPrincipalDeSpringBoot() {
		assertThat(SiraApplication.class).hasAnnotation(SpringBootApplication.class);
	}

}

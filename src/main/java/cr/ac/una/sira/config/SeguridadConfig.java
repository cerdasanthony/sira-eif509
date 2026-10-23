package cr.ac.una.sira.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import cr.ac.una.sira.presentation.ProblemasHttp;
import cr.ac.una.sira.business.AccesoAdministracionService;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.*;

@Configuration
public class SeguridadConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecretKey claveJwt(@Value("${sira.jwt.secret}") String base64) {
        byte[] bytes;
        if (base64.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(base64);
            if (bytes.length < 32) throw new IllegalArgumentException("La clave JWT requiere al menos 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey clave) { return new NimbusJwtEncoder(new ImmutableSecret<>(clave)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKey clave, @Value("${sira.jwt.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> contrato = token -> token.getAudience() != null
                && token.getAudience().contains("sira-api") && token.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia o expiracion invalidas", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), contrato));
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain administracion(HttpSecurity http, UserDetailsService cuentas,
                                      PasswordEncoder passwords, AccesoAdministracionService accesos,
                                      @Value("${sira.cors.origins:}") String origenes,
                                      @Value("${sira.portal-url}") String portalUrl) throws Exception {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(cuentas);
        provider.setPasswordEncoder(passwords);
        return http.securityMatcher("/admin/**", "/css/**")
                .addFilterBefore(new ContinuarAdministracionFilter(accesos, origenes, portalUrl), CsrfFilter.class)
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login", "/admin/acceso-denegado", "/css/**").permitAll()
                        .anyRequest().hasRole("PROFESIONAL"))
                .formLogin(form -> form.loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login").usernameParameter("correo")
                        .defaultSuccessUrl("/admin/participantes", true)
                        .failureUrl("/admin/login?error").permitAll())
                .logout(logout -> logout.logoutUrl("/admin/logout")
                        .logoutSuccessUrl(portalUrl))
                .exceptionHandling(errors -> errors.accessDeniedPage("/admin/acceso-denegado"))
                .csrf(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${sira.cors.origins:}") String origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Location"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        source.registerCorsConfiguration("/auth/**", cors);
        return source;
    }

    @Bean
    @Order(2)
    SecurityFilterChain seguridad(HttpSecurity http, ProblemasHttp problemas) throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/auth/login", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/v3/api-docs.yaml", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("PROFESIONAL", "ENCARGADO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/me").hasAnyRole("PROFESIONAL", "ENCARGADO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/me").hasAnyRole("PROFESIONAL", "ENCARGADO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ejecuciones/cierres").hasRole("ENCARGADO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/acceso/administracion").hasRole("PROFESIONAL")
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/participantes/**", "/api/v1/rutinas/**")
                            .hasRole("PROFESIONAL")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> problemas.escribir(req, res, 401,
                                "AUTENTICACION_REQUERIDA", "Se requiere un token Bearer valido"))
                        .accessDeniedHandler((req, res, ex) -> problemas.escribir(req, res, 403,
                                "ACCESO_DENEGADO", "El rol no permite esta operacion")))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((req, res, ex) -> problemas.escribir(req, res, 401,
                                "TOKEN_INVALIDO", "El token no es valido o ha expirado"))
                        .accessDeniedHandler((req, res, ex) -> problemas.escribir(req, res, 403,
                                "ACCESO_DENEGADO", "El rol no permite esta operacion")))
                .build();
    }
}

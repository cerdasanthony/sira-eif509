package cr.ac.una.sira.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import cr.ac.una.sira.presentation.ProblemasHttp;
import cr.ac.una.sira.business.AccesoAdministracionService;
import cr.ac.una.sira.business.ControlIntentosIngreso;
import cr.ac.una.sira.business.EventosSeguridad;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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

    /**
     * Swagger UI necesita estilos y scripts en linea propios, asi que lleva su propia
     * politica de contenido en lugar de debilitar la del resto de la aplicacion.
     */
    @Bean
    @Order(0)
    SecurityFilterChain documentacion(HttpSecurity http) throws Exception {
        return http.securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs",
                        "/v3/api-docs/**", "/v3/api-docs.yaml")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(this::cabecerasComunes)
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                + "script-src 'self' 'unsafe-inline'; connect-src 'self'; "
                                + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'")))
                .build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain administracion(HttpSecurity http, UserDetailsService cuentas,
                                      PasswordEncoder passwords, AccesoAdministracionService accesos,
                                      ControlIntentosIngreso intentos, EventosSeguridad eventos,
                                      @Value("${sira.cors.origins:}") String origenes,
                                      @Value("${sira.portal-url}") String portalUrl) throws Exception {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(cuentas);
        provider.setPasswordEncoder(passwords);
        return http.securityMatcher("/admin/**", "/css/**")
                .addFilterBefore(new ContinuarAdministracionFilter(accesos, origenes, portalUrl), CsrfFilter.class)
                .authenticationProvider(new ProveedorIngresoLimitado(provider, intentos, eventos))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login", "/admin/acceso-denegado", "/css/**").permitAll()
                        .anyRequest().hasRole("PROFESIONAL"))
                .formLogin(form -> form.loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login").usernameParameter("correo")
                        .defaultSuccessUrl("/admin/participantes", true)
                        .failureHandler((peticion, respuesta, error) -> respuesta.sendRedirect(
                                peticion.getContextPath() + "/admin/login"
                                        + (error instanceof LockedException ? "?bloqueado" : "?error")))
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/admin/logout")
                        .logoutSuccessUrl(portalUrl))
                .exceptionHandling(errors -> errors.accessDeniedPage("/admin/acceso-denegado"))
                .csrf(Customizer.withDefaults())
                .headers(this::cabecerasComunes)
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; "
                                + "connect-src 'self'; object-src 'none'; base-uri 'self'; "
                                + "form-action 'self'; frame-ancestors 'none'")))
                .build();
    }

    /**
     * Cabeceras que toda respuesta debe llevar: HSTS para no volver a texto plano,
     * nada de marcos ajenos, sin adivinar tipos MIME, sin referente hacia otros sitios
     * y sin permisos de camara, microfono ni ubicacion.
     */
    private void cabecerasComunes(HeadersConfigurer<HttpSecurity> cabeceras) {
        cabeceras.frameOptions(marcos -> marcos.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(referente -> referente.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
        cabeceras.permissionsPolicy(permisos -> permisos.policy(
                "camera=(), microphone=(), geolocation=(), payment=(), usb=()"));
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
        var healthCors = new CorsConfiguration();
        healthCors.setAllowedOrigins(cors.getAllowedOrigins());
        healthCors.setAllowedMethods(List.of("GET"));
        healthCors.setAllowCredentials(false);
        source.registerCorsConfiguration("/actuator/health", healthCors);
        return source;
    }

    @Bean
    @Order(2)
    SecurityFilterChain seguridad(HttpSecurity http, ProblemasHttp problemas,
                                  EventosSeguridad eventos) throws Exception {
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
                                "/v3/api-docs/**", "/v3/api-docs.yaml", "/actuator/health", "/actuator/info")
                            .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("PROFESIONAL", "ENCARGADO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/me").hasAnyRole("PROFESIONAL", "ENCARGADO")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/me").hasAnyRole("PROFESIONAL", "ENCARGADO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ejecuciones/cierres").hasRole("ENCARGADO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/acceso/administracion").hasRole("PROFESIONAL")
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/participantes/**", "/api/v1/rutinas/**")
                            .hasRole("PROFESIONAL")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            eventos.autenticacionRequerida(req.getMethod(), req.getRequestURI(), "sin token Bearer");
                            problemas.escribir(req, res, 401,
                                    "AUTENTICACION_REQUERIDA", "Se requiere un token Bearer valido");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            eventos.accesoDenegado(req.getMethod(), req.getRequestURI(), "rol insuficiente");
                            problemas.escribir(req, res, 403,
                                    "ACCESO_DENEGADO", "El rol no permite esta operacion");
                        }))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((req, res, ex) -> {
                            eventos.autenticacionRequerida(req.getMethod(), req.getRequestURI(), "token invalido");
                            problemas.escribir(req, res, 401,
                                    "TOKEN_INVALIDO", "El token no es valido o ha expirado");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            eventos.accesoDenegado(req.getMethod(), req.getRequestURI(), "rol insuficiente");
                            problemas.escribir(req, res, 403,
                                    "ACCESO_DENEGADO", "El rol no permite esta operacion");
                        }))
                .headers(this::cabecerasComunes)
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")))
                .build();
    }
}

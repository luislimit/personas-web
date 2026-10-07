package es.gestionpersonas.configuracion;

import es.gestionpersonas.seguridad.web.FiltroAutenticacionBearer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * CORS configurable para clientes web que vivan en un origen distinto al backend.
     *
     * Desarrollo por defecto:
     *   http://localhost:5175
     *   http://127.0.0.1:5175
     *
     * Producción:
     *   APP_CORS_ALLOWED_ORIGINS=https://app.ejemplo.es,https://admin.ejemplo.es
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:5175,http://127.0.0.1:5175}")
        String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList()
        );

        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            "X-Contexto-Id"
        ));

        configuration.setExposedHeaders(List.of(
            HttpHeaders.LOCATION
        ));

        // La API usa Bearer token, no cookie de sesión del navegador.
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        FiltroAutenticacionBearer filtroAutenticacionBearer
    ) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/api/sistema/salud",
                    "/api/autenticacion/iniciar-sesion",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, cause) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().printf(
                        "{\"fechaHora\":\"%s\",\"estadoHttp\":401,\"error\":\"No autorizado\",\"mensaje\":\"Autenticación requerida\",\"validacion\":{}}",
                        LocalDateTime.now().withNano(0)
                    );
                })
                .accessDeniedHandler((request, response, cause) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().printf(
                        "{\"fechaHora\":\"%s\",\"estadoHttp\":403,\"error\":\"Prohibido\",\"mensaje\":\"Permiso insuficiente\",\"validacion\":{}}",
                        LocalDateTime.now().withNano(0)
                    );
                })
            )
            .addFilterBefore(filtroAutenticacionBearer, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}

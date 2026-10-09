package com.example.banco.config;

import com.example.banco.security.ConvertidorJwtUsuario;
import com.example.banco.security.ManejadorErroresSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Seguridad de la API:
 * <ul>
 *   <li>Públicos: POST /auth/login, POST /clientes (el registro crea al usuario),
 *       catálogos, documentación OpenAPI y health.</li>
 *   <li>Todo lo demás exige un JWT válido en Authorization: Bearer.</li>
 *   <li>Sin sesión en el servidor (STATELESS) y sin CSRF, porque no hay cookies.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] GET_PUBLICOS = {
            "/catalogos/**",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
            "/actuator/health", "/actuator/health/**", "/actuator/info"
    };

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http,
                                        ConvertidorJwtUsuario convertidorJwt,
                                        ManejadorErroresSeguridad manejadorErrores) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/clientes").permitAll()
                        .requestMatchers(HttpMethod.GET, GET_PUBLICOS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt))
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores));
        return http.build();
    }

    /** BCrypt con costo configurable (12 en producción, 4 en pruebas para que sean rápidas). */
    @Bean
    PasswordEncoder passwordEncoder(SeguridadProperties seguridad) {
        return new BCryptPasswordEncoder(seguridad.bcryptFuerza());
    }

    /** CORS: solo los orígenes configurados (ninguno por omisión). */
    @Bean
    CorsConfigurationSource corsConfigurationSource(SeguridadProperties seguridad) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(seguridad.cors().origenesValidos());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        cors.setExposedHeaders(List.of("Location", "X-Request-Id", "Retry-After"));
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }
}

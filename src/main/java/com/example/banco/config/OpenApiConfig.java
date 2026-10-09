package com.example.banco.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Documentación OpenAPI 3 (Swagger UI en /swagger-ui.html). */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Banco · API de Onboarding de Clientes",
        version = "1.0.0",
        description = """
                Registro de clientes personas físicas con apertura automática de cuenta y \
                usuario de acceso. Flujo: 1) POST /clientes (público) · 2) POST /auth/login \
                para obtener el token · 3) botón Authorize con el token para el resto de endpoints."""))
@SecurityScheme(name = OpenApiConfig.SEGURIDAD_JWT, type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT", description = "Token obtenido en POST /auth/login")
public class OpenApiConfig {

    public static final String SEGURIDAD_JWT = "bearerAuth";
}

package com.example.banco.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/** Configuración de seguridad validada al arrancar (falla rápido si falta el secreto JWT). */
@Validated
@ConfigurationProperties(prefix = "app.seguridad")
public record SeguridadProperties(
        @Min(4) @Max(16)
        int bcryptFuerza,

        @NotNull @Valid
        Jwt jwt,

        @NotNull @Valid
        Login login,

        @NotNull @Valid
        Cors cors) {

    public record Jwt(
            @NotBlank(message = "Define el secreto JWT con la variable de entorno JWT_SECRET")
            @Size(min = 32, message = "El secreto JWT debe tener al menos 32 caracteres (256 bits)")
            String secreto,

            @NotBlank String emisor,

            @NotBlank String audiencia,

            @NotNull Duration expiracion) {

        @Override
        public String toString() {
            return "Jwt[emisor=" + emisor + ", audiencia=" + audiencia + ", expiracion=" + expiracion + ", secreto=****]";
        }
    }

    public record Login(
            @Min(1) int maxIntentos,
            @NotNull Duration bloqueo) {
    }

    public record Cors(List<String> origenesPermitidos) {

        public List<String> origenesValidos() {
            return origenesPermitidos == null ? List.of()
                    : origenesPermitidos.stream().map(String::strip).filter(o -> !o.isEmpty()).toList();
        }
    }
}

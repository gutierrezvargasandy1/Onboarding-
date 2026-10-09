package com.example.banco.dto.request;

import com.example.banco.util.Texto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.example.banco.util.Constantes.CAMPO_OBLIGATORIO;

@Schema(description = "Credenciales: el nombre de usuario es el correo registrado")
public record LoginRequest(
        @Schema(example = "maria.lopez@ejemplo.com")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String correo,

        @Schema(example = "Segura#2026", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 200, message = "demasiado larga")
        String password
) {
    public LoginRequest {
        correo = Texto.minusculas(correo);
    }

    @Override
    public String toString() {
        return "LoginRequest[correo=" + correo + ", password=****]";
    }
}

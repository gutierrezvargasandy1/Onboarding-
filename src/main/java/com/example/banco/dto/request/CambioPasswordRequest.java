package com.example.banco.dto.request;

import com.example.banco.validation.annotation.PasswordSegura;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.example.banco.util.Constantes.CAMPO_OBLIGATORIO;

@Schema(description = "Cambio de contraseña: se pide la actual para confirmar la identidad")
public record CambioPasswordRequest(
        @Schema(example = "Segura#2026", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 200, message = "demasiado larga")
        String passwordActual,

        @Schema(example = "NuevaClave#2026", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = CAMPO_OBLIGATORIO) @PasswordSegura
        String passwordNueva
) {
    @Override
    public String toString() {
        return "CambioPasswordRequest[****]";
    }
}

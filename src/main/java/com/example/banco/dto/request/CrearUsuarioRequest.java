package com.example.banco.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearUsuarioRequest(
        @NotNull(message = "El ID del cliente es obligatorio") Integer clienteId,

        @NotBlank(message = "La contraseña es obligatoria") String password) {
}
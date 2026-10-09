package com.example.banco.dto.response;

import java.time.OffsetDateTime;

/** Usuario de acceso sin la contraseña. */
public record UsuarioResponse(
        Integer id,
        Integer clienteId,
        String correo,
        boolean activo,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion) {
}

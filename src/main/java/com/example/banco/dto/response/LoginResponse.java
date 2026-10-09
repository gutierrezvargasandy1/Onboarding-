package com.example.banco.dto.response;

import java.time.Instant;

/** Token JWT para enviar en el encabezado Authorization: Bearer &lt;token&gt;. */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        Instant expiraEn,
        Integer usuarioId,
        Integer clienteId,
        String correo) {
}

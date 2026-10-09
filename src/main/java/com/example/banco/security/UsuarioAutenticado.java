package com.example.banco.security;

import org.springframework.security.core.AuthenticatedPrincipal;

/** Usuario dueño del token JWT, disponible en los controladores con @AuthenticationPrincipal. */
public record UsuarioAutenticado(Integer usuarioId, Integer clienteId, String correo) implements AuthenticatedPrincipal {

    /** El nombre del principal es el id: los logs no llevan el correo (dato personal). */
    @Override
    public String getName() {
        return String.valueOf(usuarioId);
    }
}

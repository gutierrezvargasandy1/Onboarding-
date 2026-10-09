package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** El usuario existe y sus credenciales son correctas, pero está inactivo. */
public class UsuarioInactivoException extends NegocioException {

    public UsuarioInactivoException() {
        super(HttpStatus.FORBIDDEN, "USUARIO_INACTIVO", "El usuario está inactivo: el cliente fue dado de baja");
    }
}

package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** No existe un usuario con ese id. */
public class UsuarioNoEncontradoException extends NegocioException {

    public UsuarioNoEncontradoException(Integer id) {
        super(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", "No existe un usuario con id " + id);
    }
}

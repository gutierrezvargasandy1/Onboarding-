package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** El correo ya pertenece a otro cliente o usuario. */
public class CorreoDuplicadoException extends NegocioException {

    public CorreoDuplicadoException() {
        super(HttpStatus.CONFLICT, "CORREO_DUPLICADO", "Ya existe un cliente o usuario registrado con ese correo electrónico");
    }
}

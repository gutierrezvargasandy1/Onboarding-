package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** Operación no permitida sobre un cliente dado de baja. */
public class ClienteInactivoException extends NegocioException {

    public ClienteInactivoException(Integer id) {
        super(HttpStatus.CONFLICT, "CLIENTE_INACTIVO", "El cliente " + id + " está dado de baja y no puede modificarse");
    }
}

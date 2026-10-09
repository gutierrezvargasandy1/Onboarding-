package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** Otra persona ya está registrada con ese RFC. */
public class RfcDuplicadoException extends NegocioException {

    public RfcDuplicadoException() {
        super(HttpStatus.CONFLICT, "RFC_DUPLICADO", "Ya existe un cliente registrado con ese RFC");
    }
}

package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** Otra persona ya está registrada con esa CURP. */
public class CurpDuplicadaException extends NegocioException {

    public CurpDuplicadaException() {
        super(HttpStatus.CONFLICT, "CURP_DUPLICADA", "Ya existe un cliente registrado con esa CURP");
    }
}

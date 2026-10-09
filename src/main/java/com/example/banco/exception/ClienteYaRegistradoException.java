package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/** El cliente ya existe: misma CURP y mismo RFC. */
public class ClienteYaRegistradoException extends NegocioException {

    public ClienteYaRegistradoException() {
        super(HttpStatus.CONFLICT, "CLIENTE_YA_REGISTRADO", "El cliente ya está registrado (misma CURP y RFC)");
    }
}

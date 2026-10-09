package com.example.banco.exception;

import org.springframework.http.HttpStatus;

/**
 * Correo o contraseña incorrectos. El mensaje es el mismo en ambos casos para no
 * revelar qué correos están registrados.
 */
public class CredencialesInvalidasException extends NegocioException {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
    }
}

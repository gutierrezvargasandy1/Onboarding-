package com.example.banco.security;

import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/** Token con firma válida que ya no debe aceptarse (usuario inactivo, contraseña cambiada...). */
public class TokenRechazadoException extends InvalidBearerTokenException {

    public TokenRechazadoException(String motivo) {
        super(motivo);
    }
}

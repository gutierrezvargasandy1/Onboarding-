package com.example.banco.exception;

/** Error de validación de un campo concreto de la petición. */
public record ErrorCampo(String campo, String mensaje) {
}

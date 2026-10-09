package com.example.banco.dto.response;

/** Entidad federativa: el id es la clave INEGI que se envía como domicilio.estadoId. */
public record EstadoResponse(Short id, String nombre) {
}

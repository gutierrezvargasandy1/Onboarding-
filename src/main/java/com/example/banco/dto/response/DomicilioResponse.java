package com.example.banco.dto.response;

public record DomicilioResponse(
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String municipio,
        Short estadoId,
        String estado,
        String codigoPostal,
        String pais) {
}

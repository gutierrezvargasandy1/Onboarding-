package com.example.banco.dto.response;

import com.example.banco.entity.enums.EstatusCuenta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CuentaResponse(
        Integer id,
        String numeroCuenta,
        Integer clienteId,
        BigDecimal saldo,
        String moneda,
        EstatusCuenta estatus,
        OffsetDateTime fechaApertura,
        OffsetDateTime fechaActualizacion) {
}

package com.example.banco.dto.response;

import com.example.banco.entity.enums.EstatusCuenta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaldoResponse(
        String numeroCuenta,
        BigDecimal saldo,
        String moneda,
        EstatusCuenta estatus,
        OffsetDateTime fechaConsulta) {
}

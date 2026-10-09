package com.example.banco.mapper;

import com.example.banco.dto.response.CuentaResponse;
import com.example.banco.dto.response.SaldoResponse;
import com.example.banco.entity.Cuenta;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CuentaMapper {

    public CuentaResponse aRespuesta(Cuenta cuenta) {
        return new CuentaResponse(cuenta.getId(), cuenta.getNumeroCuenta(), cuenta.getClienteId(),
                cuenta.getSaldo(), cuenta.getMoneda(), cuenta.getEstatus(),
                cuenta.getFechaApertura(), cuenta.getFechaActualizacion());
    }

    public SaldoResponse aSaldo(Cuenta cuenta) {
        return new SaldoResponse(cuenta.getNumeroCuenta(), cuenta.getSaldo(), cuenta.getMoneda(),
                cuenta.getEstatus(), OffsetDateTime.now(ZoneOffset.UTC));
    }
}

package com.example.banco.dto.response;

import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.Sexo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Representación del cliente. Nunca incluye la contraseña ni datos del usuario de acceso. */
public record ClienteResponse(
        Integer id,
        String nombre,
        String segundoNombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        LocalDate fechaNacimiento,
        String curp,
        String rfc,
        Sexo sexo,
        String nacionalidad,
        EstadoCivil estadoCivil,
        String correo,
        String telefonoMovil,
        String telefonoAlterno,
        DomicilioResponse domicilio,
        String ocupacion,
        String empresa,
        BigDecimal ingresoMensual,
        boolean activo,
        OffsetDateTime fechaRegistro,
        OffsetDateTime fechaActualizacion,
        OffsetDateTime fechaBaja,
        List<CuentaResponse> cuentas) {
}

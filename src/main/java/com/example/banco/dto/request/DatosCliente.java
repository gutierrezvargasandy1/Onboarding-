package com.example.banco.dto.request;

import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.Sexo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos modificables del cliente, comunes al alta y a la actualización (un solo mapeo). */
public interface DatosCliente {

    String nombre();

    String segundoNombre();

    String apellidoPaterno();

    String apellidoMaterno();

    LocalDate fechaNacimiento();

    Sexo sexo();

    String nacionalidad();

    EstadoCivil estadoCivil();

    String correo();

    String telefonoMovil();

    String telefonoAlterno();

    DomicilioRequest domicilio();

    String ocupacion();

    String empresa();

    BigDecimal ingresoMensual();
}

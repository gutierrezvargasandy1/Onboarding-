package com.example.banco.dto.request;

import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.Sexo;
import com.example.banco.util.Texto;
import com.example.banco.validation.annotation.CodigoPais;
import com.example.banco.validation.annotation.CorreoElectronico;
import com.example.banco.validation.annotation.MayorDeEdad;
import com.example.banco.validation.annotation.NombrePersona;
import com.example.banco.validation.annotation.Telefono;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.example.banco.util.Constantes.CAMPO_OBLIGATORIO;

/**
 * Actualización completa (PUT) de datos personales, de contacto, domicilio e
 * información laboral. CURP, RFC y número de cuenta NO se modifican: si vienen en
 * el JSON deben ser iguales a los registrados o la petición se rechaza. Se ignoran
 * los campos de solo lectura (id, activo, cuentas...), así que se puede reenviar el
 * JSON obtenido con GET después de editarlo.
 */
@Schema(description = "Datos modificables del cliente")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteActualizacionRequest(
        @Schema(example = "María")
        @NotBlank(message = CAMPO_OBLIGATORIO) @NombrePersona
        String nombre,

        @Schema(example = "Fernanda", nullable = true)
        @NombrePersona
        String segundoNombre,

        @Schema(example = "López")
        @NotBlank(message = CAMPO_OBLIGATORIO) @NombrePersona
        String apellidoPaterno,

        @Schema(example = "Hernández")
        @NotBlank(message = CAMPO_OBLIGATORIO) @NombrePersona
        String apellidoMaterno,

        @Schema(example = "1995-08-21")
        @NotNull(message = CAMPO_OBLIGATORIO) @MayorDeEdad
        LocalDate fechaNacimiento,

        @Schema(example = "M")
        @NotNull(message = CAMPO_OBLIGATORIO)
        Sexo sexo,

        @Schema(example = "MX")
        @NotBlank(message = CAMPO_OBLIGATORIO) @CodigoPais
        String nacionalidad,

        @Schema(example = "CASADO")
        @NotNull(message = CAMPO_OBLIGATORIO)
        EstadoCivil estadoCivil,

        @Schema(example = "maria.lopez@ejemplo.com", description = "Si cambia, también cambia el nombre de usuario")
        @NotBlank(message = CAMPO_OBLIGATORIO) @CorreoElectronico
        String correo,

        @Schema(example = "4731234567")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Telefono
        String telefonoMovil,

        @Schema(example = "4737654321", nullable = true)
        @Telefono
        String telefonoAlterno,

        @NotNull(message = CAMPO_OBLIGATORIO) @Valid
        DomicilioRequest domicilio,

        @Schema(example = "Líder técnica")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String ocupacion,

        @Schema(example = "Tecnologías del Bajío SA de CV")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String empresa,

        @Schema(example = "42000.00")
        @NotNull(message = CAMPO_OBLIGATORIO)
        @DecimalMin(value = "0.00", inclusive = false, message = "debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "admite hasta 10 enteros y 2 decimales")
        BigDecimal ingresoMensual,

        @Schema(nullable = true, description = "No modificable. Opcional: si se envía debe ser la CURP actual")
        String curp,

        @Schema(nullable = true, description = "No modificable. Opcional: si se envía debe ser el RFC actual")
        String rfc,

        @Schema(nullable = true, description = "No modificable. Opcional: si se envía debe ser una cuenta del cliente")
        String numeroCuenta
) implements DatosCliente {

    public ClienteActualizacionRequest {
        nombre = Texto.limpiar(nombre);
        segundoNombre = Texto.limpiar(segundoNombre);
        apellidoPaterno = Texto.limpiar(apellidoPaterno);
        apellidoMaterno = Texto.limpiar(apellidoMaterno);
        nacionalidad = Texto.mayusculas(nacionalidad);
        correo = Texto.minusculas(correo);
        telefonoMovil = Texto.limpiar(telefonoMovil);
        telefonoAlterno = Texto.limpiar(telefonoAlterno);
        ocupacion = Texto.limpiar(ocupacion);
        empresa = Texto.limpiar(empresa);
        curp = Texto.mayusculas(curp);
        rfc = Texto.mayusculas(rfc);
        numeroCuenta = Texto.limpiar(numeroCuenta);
    }
}

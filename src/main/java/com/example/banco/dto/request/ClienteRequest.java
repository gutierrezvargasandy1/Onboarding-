package com.example.banco.dto.request;

import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.Sexo;
import com.example.banco.util.Texto;
import com.example.banco.validation.annotation.CodigoPais;
import com.example.banco.validation.annotation.CorreoElectronico;
import com.example.banco.validation.annotation.Curp;
import com.example.banco.validation.annotation.MayorDeEdad;
import com.example.banco.validation.annotation.NombrePersona;
import com.example.banco.validation.annotation.PasswordSegura;
import com.example.banco.validation.annotation.Rfc;
import com.example.banco.validation.annotation.Telefono;
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
 * Alta de cliente. El constructor normaliza los textos (espacios, mayúsculas en
 * CURP/RFC, minúsculas en el correo) antes de que se ejecuten las validaciones.
 */
@Schema(description = "Datos para registrar un cliente persona física")
public record ClienteRequest(
        @Schema(example = "María")
        @NotBlank(message = CAMPO_OBLIGATORIO) @NombrePersona
        String nombre,

        @Schema(example = "Fernanda", nullable = true, description = "Opcional")
        @NombrePersona
        String segundoNombre,

        @Schema(example = "López")
        @NotBlank(message = CAMPO_OBLIGATORIO) @NombrePersona
        String apellidoPaterno,

        @Schema(example = "Hernández")
        @NotBlank(message = CAMPO_OBLIGATORIO) @NombrePersona
        String apellidoMaterno,

        @Schema(example = "1995-08-21", description = "AAAA-MM-DD; debe coincidir con la fecha de la CURP")
        @NotNull(message = CAMPO_OBLIGATORIO) @MayorDeEdad
        LocalDate fechaNacimiento,

        @Schema(example = "LOHF950821MGTPRR08")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Curp
        String curp,

        @Schema(example = "LOHF950821QK5")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Rfc
        String rfc,

        @Schema(example = "M", description = "H, M o X")
        @NotNull(message = CAMPO_OBLIGATORIO)
        Sexo sexo,

        @Schema(example = "MX", description = "Código ISO 3166-1 alfa-2. Ver GET /catalogos/paises")
        @NotBlank(message = CAMPO_OBLIGATORIO) @CodigoPais
        String nacionalidad,

        @Schema(example = "SOLTERO")
        @NotNull(message = CAMPO_OBLIGATORIO)
        EstadoCivil estadoCivil,

        @Schema(example = "maria.lopez@ejemplo.com", description = "También será el nombre de usuario")
        @NotBlank(message = CAMPO_OBLIGATORIO) @CorreoElectronico
        String correo,

        @Schema(example = "4731234567")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Telefono
        String telefonoMovil,

        @Schema(example = "4737654321", nullable = true, description = "Opcional")
        @Telefono
        String telefonoAlterno,

        @NotNull(message = CAMPO_OBLIGATORIO) @Valid
        DomicilioRequest domicilio,

        @Schema(example = "Ingeniera de software")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String ocupacion,

        @Schema(example = "Tecnologías del Bajío SA de CV")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String empresa,

        @Schema(example = "35000.00")
        @NotNull(message = CAMPO_OBLIGATORIO)
        @DecimalMin(value = "0.00", inclusive = false, message = "debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "admite hasta 10 enteros y 2 decimales")
        BigDecimal ingresoMensual,

        @Schema(example = "Segura#2026", accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "Mínimo 8 caracteres con mayúscula, minúscula, número y carácter especial")
        @NotBlank(message = CAMPO_OBLIGATORIO) @PasswordSegura
        String password
) implements DatosCliente {

    public ClienteRequest {
        nombre = Texto.limpiar(nombre);
        segundoNombre = Texto.limpiar(segundoNombre);
        apellidoPaterno = Texto.limpiar(apellidoPaterno);
        apellidoMaterno = Texto.limpiar(apellidoMaterno);
        curp = Texto.mayusculas(curp);
        rfc = Texto.mayusculas(rfc);
        nacionalidad = Texto.mayusculas(nacionalidad);
        correo = Texto.minusculas(correo);
        telefonoMovil = Texto.limpiar(telefonoMovil);
        telefonoAlterno = Texto.limpiar(telefonoAlterno);
        ocupacion = Texto.limpiar(ocupacion);
        empresa = Texto.limpiar(empresa);
    }

    /** Nunca imprime la contraseña. */
    @Override
    public String toString() {
        return "ClienteRequest[curp=" + curp + ", correo=" + correo + ", password=****]";
    }
}

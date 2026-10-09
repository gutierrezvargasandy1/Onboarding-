package com.example.banco.dto.request;

import com.example.banco.util.Constantes;
import com.example.banco.util.Texto;
import com.example.banco.validation.annotation.CodigoPostal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.example.banco.util.Constantes.CAMPO_OBLIGATORIO;

@Schema(description = "Domicilio en México")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DomicilioRequest(
        @Schema(example = "Calle Hidalgo")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String calle,

        @Schema(example = "123")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 10, message = "debe tener máximo 10 caracteres")
        String numeroExterior,

        @Schema(example = "4B", nullable = true, description = "Opcional")
        @Size(max = 10, message = "debe tener máximo 10 caracteres")
        String numeroInterior,

        @Schema(example = "Centro")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String colonia,

        @Schema(example = "Guanajuato")
        @NotBlank(message = CAMPO_OBLIGATORIO) @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String municipio,

        @Schema(example = "11", description = "Clave INEGI de la entidad federativa (1-32). Ver GET /catalogos/estados")
        @NotNull(message = CAMPO_OBLIGATORIO) @Min(value = 1, message = "debe estar entre 1 y 32")
        @Max(value = 32, message = "debe estar entre 1 y 32")
        Short estadoId,

        @Schema(example = "36000")
        @NotBlank(message = CAMPO_OBLIGATORIO) @CodigoPostal
        String codigoPostal,

        @Schema(example = "MX", defaultValue = "MX", description = "Por ahora solo México (MX); si se omite se asume MX")
        @Pattern(regexp = Constantes.PAIS_DOMICILIO, message = "solo se admiten domicilios en México (MX)")
        String pais
) {
    public DomicilioRequest {
        calle = Texto.limpiar(calle);
        numeroExterior = Texto.limpiar(numeroExterior);
        numeroInterior = Texto.limpiar(numeroInterior);
        colonia = Texto.limpiar(colonia);
        municipio = Texto.limpiar(municipio);
        codigoPostal = Texto.limpiar(codigoPostal);
        pais = pais == null || pais.isBlank() ? Constantes.PAIS_DOMICILIO : Texto.mayusculas(pais);
    }
}

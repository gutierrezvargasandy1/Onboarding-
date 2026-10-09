package com.example.banco.dto.response;

import com.example.banco.exception.ErrorCampo;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Formato único de error, basado en RFC 9457 "Problem Details for HTTP APIs"
 * (title, status, detail, instance) con extensiones: codigo estable,
 * timestamp, requestId para rastrear en los logs y errores por campo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error (RFC 9457)")
public record RespuestaError(
        @Schema(example = "Conflicto") String title,
        @Schema(example = "409") int status,
        @Schema(example = "Ya existe un cliente registrado con esa CURP") String detail,
        @Schema(example = "/clientes") String instance,
        @Schema(example = "CURP_DUPLICADA") String codigo,
        OffsetDateTime timestamp,
        String requestId,
        List<ErrorCampo> errores) {
}

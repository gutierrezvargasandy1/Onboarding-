package com.example.banco.controller;

import com.example.banco.config.OpenApiConfig;
import com.example.banco.dto.response.CuentaResponse;
import com.example.banco.dto.response.PaginaResponse;
import com.example.banco.dto.response.RespuestaError;
import com.example.banco.dto.response.SaldoResponse;
import com.example.banco.service.CuentaService;
import com.example.banco.web.Ordenamiento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cuentas", description = "Consulta de cuentas y saldos")
@SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@ApiResponse(responseCode = "404", description = "Cuenta no encontrada",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
public class CuentaController {

    private final CuentaService cuentaService;

    @Operation(summary = "Consultar cuentas activas")
    @GetMapping("/activas")
    public PaginaResponse<CuentaResponse> listarActivas(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pagina) {
        return cuentaService.listarActivas(Ordenamiento.validar(pagina, Ordenamiento.CUENTAS));
    }

    @Operation(summary = "Consultar cuenta por número")
    @GetMapping("/{numeroCuenta}")
    public CuentaResponse obtener(@Parameter(example = "1000000016") @PathVariable String numeroCuenta) {
        return cuentaService.obtener(numeroCuenta);
    }

    @Operation(summary = "Consultar saldo de una cuenta")
    @GetMapping("/{numeroCuenta}/saldo")
    public SaldoResponse consultarSaldo(@Parameter(example = "1000000016") @PathVariable String numeroCuenta) {
        return cuentaService.consultarSaldo(numeroCuenta);
    }
}

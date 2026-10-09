package com.example.banco.controller;

import com.example.banco.config.OpenApiConfig;
import com.example.banco.dto.request.ClienteActualizacionRequest;
import com.example.banco.dto.request.ClienteRequest;
import com.example.banco.dto.response.ClienteResponse;
import com.example.banco.dto.response.PaginaResponse;
import com.example.banco.dto.response.RespuestaError;
import com.example.banco.service.ClienteService;
import com.example.banco.web.Ordenamiento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@Tag(name = "Clientes", description = "Registro, consultas, actualización y baja lógica de clientes")
@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@ApiResponse(responseCode = "400", description = "Datos inválidos",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Registrar cliente",
            description = "Público. En una sola transacción crea el cliente, su domicilio, una cuenta ACTIVA "
                    + "con el saldo inicial del sistema y su usuario de acceso (correo + contraseña BCrypt).")
    @ApiResponse(responseCode = "201", description = "Cliente registrado; encabezado Location con su URL")
    @ApiResponse(responseCode = "409", description = "Cliente ya registrado, CURP, RFC o correo duplicado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest solicitud) {
        ClienteResponse creado = clienteService.registrar(solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Consultar todos los clientes", description = "Paginado: ?page=0&size=20&sort=id,asc")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @GetMapping
    public PaginaResponse<ClienteResponse> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pagina) {
        return clienteService.listar(Ordenamiento.validar(pagina, Ordenamiento.CLIENTES));
    }

    @Operation(summary = "Consultar clientes activos")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @GetMapping("/activos")
    public PaginaResponse<ClienteResponse> listarActivos(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pagina) {
        return clienteService.listarActivos(Ordenamiento.validar(pagina, Ordenamiento.CLIENTES));
    }

    @Operation(summary = "Clientes registrados en un rango de fechas",
            description = "Fechas AAAA-MM-DD, ambas incluidas (hora de la Ciudad de México)")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @GetMapping("/registrados")
    public PaginaResponse<ClienteResponse> listarRegistradosEntre(
            @Parameter(example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(example = "2026-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.ASC) Pageable pagina) {
        return clienteService.listarRegistradosEntre(desde, hasta, Ordenamiento.validar(pagina, Ordenamiento.CLIENTES));
    }

    @Operation(summary = "Consultar cliente por ID")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable @Positive(message = "debe ser mayor a cero") Integer id) {
        return clienteService.obtenerPorId(id);
    }

    @Operation(summary = "Buscar cliente por CURP")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @GetMapping("/curp/{curp}")
    public ClienteResponse obtenerPorCurp(@Parameter(example = "LOHF950821MGTPRR08") @PathVariable String curp) {
        return clienteService.obtenerPorCurp(curp);
    }

    @Operation(summary = "Buscar cliente por RFC")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @GetMapping("/rfc/{rfc}")
    public ClienteResponse obtenerPorRfc(@Parameter(example = "LOHF950821QK5") @PathVariable String rfc) {
        return clienteService.obtenerPorRfc(rfc);
    }

    @Operation(summary = "Buscar cliente por correo electrónico")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @GetMapping("/correo/{correo}")
    public ClienteResponse obtenerPorCorreo(@Parameter(example = "maria.lopez@ejemplo.com") @PathVariable String correo) {
        return clienteService.obtenerPorCorreo(correo);
    }

    @Operation(summary = "Consultar cliente por número de cuenta")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @GetMapping("/cuenta/{numeroCuenta}")
    public ClienteResponse obtenerPorNumeroCuenta(@Parameter(example = "1000000016") @PathVariable String numeroCuenta) {
        return clienteService.obtenerPorNumeroCuenta(numeroCuenta);
    }

    @Operation(summary = "Actualizar cliente",
            description = "Reemplaza datos personales, de contacto, domicilio e información laboral. "
                    + "CURP, RFC y número de cuenta no se pueden modificar.")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @ApiResponse(responseCode = "409", description = "Correo duplicado, cliente dado de baja o modificación simultánea",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ClienteResponse actualizar(@PathVariable @Positive(message = "debe ser mayor a cero") Integer id,
                                      @Valid @RequestBody ClienteActualizacionRequest solicitud) {
        return clienteService.actualizar(id, solicitud);
    }

    @Operation(summary = "Dar de baja (baja lógica)",
            description = "No borra el registro: marca al cliente inactivo, sus cuentas pasan a INACTIVA y su usuario queda inactivo.")
    @SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
    @ApiResponse(responseCode = "204", description = "Cliente dado de baja")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable @Positive(message = "debe ser mayor a cero") Integer id) {
        clienteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}

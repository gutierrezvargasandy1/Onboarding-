package com.example.banco.controller;

import com.example.banco.dto.request.LoginRequest;
import com.example.banco.dto.response.LoginResponse;
import com.example.banco.dto.response.RespuestaError;
import com.example.banco.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Inicio de sesión y emisión del token JWT")
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión",
            description = "Público. Recibe correo y contraseña; devuelve un JWT para el encabezado Authorization: Bearer.")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas")
    @ApiResponse(responseCode = "400", description = "Datos incompletos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "403", description = "Usuario inactivo (cliente dado de baja)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return authService.iniciarSesion(solicitud);
    }
}

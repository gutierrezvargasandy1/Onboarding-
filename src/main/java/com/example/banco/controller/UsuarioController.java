package com.example.banco.controller;

import com.example.banco.config.OpenApiConfig;
import com.example.banco.dto.request.CambioPasswordRequest;
import com.example.banco.dto.request.CrearUsuarioRequest;
import com.example.banco.dto.response.RespuestaError;
import com.example.banco.dto.response.UsuarioResponse;
import com.example.banco.entity.Usuario;
import com.example.banco.security.UsuarioAutenticado;
import com.example.banco.service.UsuarioService;
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

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuarios", description = "Usuario de acceso del cliente autenticado")
@SecurityRequirement(name = OpenApiConfig.SEGURIDAD_JWT)
@RestController
@RequestMapping(value = "/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@ApiResponse(responseCode = "403", description = "El usuario no es el dueño del token", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
@ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
public class UsuarioController {

        private final UsuarioService usuarioService;

        @Operation(summary = "Consultar usuario", description = "Solo el propio usuario. Nunca devuelve la contraseña.")
        @GetMapping("/{id}")
        public UsuarioResponse obtener(@PathVariable @Positive(message = "debe ser mayor a cero") Integer id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
                return usuarioService.obtener(id, usuario);
        }

        @Operation(summary = "Cambiar contraseña", description = "Pide la contraseña actual. Los tokens emitidos antes del cambio dejan de funcionar.")
        @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
        @PutMapping(value = "/{id}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> cambiarPassword(
                        @PathVariable @Positive(message = "debe ser mayor a cero") Integer id,
                        @Valid @RequestBody CambioPasswordRequest solicitud,
                        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
                usuarioService.cambiarPassword(id, solicitud, usuario);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Crear usuario", description = "Crea el usuario de acceso asociado a un cliente existente")
        @ApiResponse(responseCode = "201", description = "Usuario creado con éxito", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UsuarioResponse.class)))
        @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = RespuestaError.class)))
        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest solicitud) {
                UsuarioResponse respuesta = usuarioService.crearUsuarioDesdeDto(solicitud);
                URI location = URI.create("/usuarios/" + respuesta.id());
                return ResponseEntity.created(location).body(respuesta);
        }
}

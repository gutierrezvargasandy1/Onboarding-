package com.example.banco.controller;

import com.example.banco.dto.response.EstadoResponse;
import com.example.banco.dto.response.OpcionCatalogo;
import com.example.banco.dto.response.PaisResponse;
import com.example.banco.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** Catálogos públicos para llenar formularios (cambian muy poco: se permite caché de 1 hora). */
@Tag(name = "Catálogos", description = "Valores válidos para sexo, estado civil, estados, países y estatus")
@RestController
@RequestMapping(value = "/catalogos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CatalogoController {

    private static final CacheControl UNA_HORA = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final CatalogoService catalogoService;

    @Operation(summary = "Entidades federativas (clave INEGI para domicilio.estadoId)")
    @GetMapping("/estados")
    public ResponseEntity<List<EstadoResponse>> estados() {
        return ResponseEntity.ok().cacheControl(UNA_HORA).body(catalogoService.estados());
    }

    @Operation(summary = "Países ISO 3166-1 (código para nacionalidad)")
    @GetMapping("/paises")
    public ResponseEntity<List<PaisResponse>> paises() {
        return ResponseEntity.ok().cacheControl(UNA_HORA).body(catalogoService.paises());
    }

    @Operation(summary = "Sexo")
    @GetMapping("/sexos")
    public ResponseEntity<List<OpcionCatalogo>> sexos() {
        return ResponseEntity.ok().cacheControl(UNA_HORA).body(catalogoService.sexos());
    }

    @Operation(summary = "Estado civil")
    @GetMapping("/estados-civiles")
    public ResponseEntity<List<OpcionCatalogo>> estadosCiviles() {
        return ResponseEntity.ok().cacheControl(UNA_HORA).body(catalogoService.estadosCiviles());
    }

    @Operation(summary = "Estatus de cuenta")
    @GetMapping("/estatus-cuenta")
    public ResponseEntity<List<OpcionCatalogo>> estatusCuenta() {
        return ResponseEntity.ok().cacheControl(UNA_HORA).body(catalogoService.estatusCuenta());
    }
}

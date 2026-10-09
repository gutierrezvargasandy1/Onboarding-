package com.example.banco.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Catálogo cat_estatus_cuenta. Toda cuenta nueva inicia ACTIVA. */
@Getter
@RequiredArgsConstructor
public enum EstatusCuenta implements CatalogoEnum {
    ACTIVA((short) 1, "Activa"),
    INACTIVA((short) 2, "Inactiva"),
    BLOQUEADA((short) 3, "Bloqueada"),
    CANCELADA((short) 4, "Cancelada");

    private final short id;
    private final String descripcion;

    @JsonCreator
    public static EstatusCuenta desdeTexto(String valor) {
        return CatalogoEnum.desdeTexto(EstatusCuenta.class, valor, "estatus");
    }
}

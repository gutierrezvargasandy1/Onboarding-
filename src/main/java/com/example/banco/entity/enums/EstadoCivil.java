package com.example.banco.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Catálogo cat_estado_civil. */
@Getter
@RequiredArgsConstructor
public enum EstadoCivil implements CatalogoEnum {
    SOLTERO((short) 1, "Soltero(a)"),
    CASADO((short) 2, "Casado(a)"),
    UNION_LIBRE((short) 3, "Unión libre"),
    DIVORCIADO((short) 4, "Divorciado(a)"),
    VIUDO((short) 5, "Viudo(a)"),
    SEPARADO((short) 6, "Separado(a)");

    private final short id;
    private final String descripcion;

    @JsonCreator
    public static EstadoCivil desdeTexto(String valor) {
        return CatalogoEnum.desdeTexto(EstadoCivil.class, valor, "estadoCivil");
    }
}

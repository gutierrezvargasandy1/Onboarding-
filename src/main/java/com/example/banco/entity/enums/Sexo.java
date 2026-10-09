package com.example.banco.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Catálogo cat_sexo: 1 = H, 2 = M, 3 = X. */
@Getter
@RequiredArgsConstructor
public enum Sexo implements CatalogoEnum {
    H((short) 1, "Hombre"),
    M((short) 2, "Mujer"),
    X((short) 3, "No binario");

    private final short id;
    private final String descripcion;

    @JsonCreator
    public static Sexo desdeTexto(String valor) {
        return CatalogoEnum.desdeTexto(Sexo.class, valor, "sexo");
    }
}

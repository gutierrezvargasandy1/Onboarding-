package com.example.banco.entity.enums;

import com.example.banco.exception.ValorCatalogoInvalidoException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Enum respaldado por un catálogo de la base de datos con id fijo (SMALLINT).
 * Una prueba de integración verifica que ids y claves coincidan con las tablas cat_*.
 */
public interface CatalogoEnum {

    short getId();

    String getDescripcion();

    /** Convierte la clave ("H", "soltero") o el id ("1") recibido en el JSON. */
    static <E extends Enum<E> & CatalogoEnum> E desdeTexto(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.strip().toUpperCase(Locale.ROOT);
        for (E opcion : tipo.getEnumConstants()) {
            if (opcion.name().equals(normalizado) || String.valueOf(opcion.getId()).equals(normalizado)) {
                return opcion;
            }
        }
        String permitidos = String.join(", ", Arrays.stream(tipo.getEnumConstants()).map(Enum::name).toList());
        throw new ValorCatalogoInvalidoException(campo, valor, permitidos);
    }

    static <E extends Enum<E> & CatalogoEnum> E desdeId(Class<E> tipo, short id) {
        for (E opcion : tipo.getEnumConstants()) {
            if (opcion.getId() == id) {
                return opcion;
            }
        }
        throw new IllegalStateException("Id " + id + " no existe en " + tipo.getSimpleName());
    }
}

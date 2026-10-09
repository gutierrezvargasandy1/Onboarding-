package com.example.banco.entity.converter;

import com.example.banco.entity.enums.CatalogoEnum;
import jakarta.persistence.AttributeConverter;

/** Guarda un enum de catálogo como su id SMALLINT (2 bytes) en lugar de texto. */
public abstract class CatalogoConverter<E extends Enum<E> & CatalogoEnum> implements AttributeConverter<E, Short> {

    private final Class<E> tipo;

    protected CatalogoConverter(Class<E> tipo) {
        this.tipo = tipo;
    }

    @Override
    public Short convertToDatabaseColumn(E valor) {
        return valor == null ? null : valor.getId();
    }

    @Override
    public E convertToEntityAttribute(Short id) {
        return id == null ? null : CatalogoEnum.desdeId(tipo, id);
    }
}

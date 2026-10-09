package com.example.banco.entity.converter;

import com.example.banco.entity.enums.Sexo;
import jakarta.persistence.Converter;

@Converter
public class SexoConverter extends CatalogoConverter<Sexo> {

    public SexoConverter() {
        super(Sexo.class);
    }
}

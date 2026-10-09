package com.example.banco.entity.converter;

import com.example.banco.entity.enums.EstadoCivil;
import jakarta.persistence.Converter;

@Converter
public class EstadoCivilConverter extends CatalogoConverter<EstadoCivil> {

    public EstadoCivilConverter() {
        super(EstadoCivil.class);
    }
}

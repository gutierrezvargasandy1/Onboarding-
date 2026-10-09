package com.example.banco.entity.converter;

import com.example.banco.entity.enums.EstatusCuenta;
import jakarta.persistence.Converter;

@Converter
public class EstatusCuentaConverter extends CatalogoConverter<EstatusCuenta> {

    public EstatusCuentaConverter() {
        super(EstatusCuenta.class);
    }
}

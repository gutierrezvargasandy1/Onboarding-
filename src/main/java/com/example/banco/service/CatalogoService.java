package com.example.banco.service;

import com.example.banco.dto.response.EstadoResponse;
import com.example.banco.dto.response.OpcionCatalogo;
import com.example.banco.dto.response.PaisResponse;
import com.example.banco.entity.CatEstado;
import com.example.banco.entity.enums.CatalogoEnum;
import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.EstatusCuenta;
import com.example.banco.entity.enums.Sexo;
import com.example.banco.exception.ErrorValidacionException;
import com.example.banco.repository.CatEstadoRepository;
import com.example.banco.repository.CatPaisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/** Catálogos de solo lectura y validación de claves contra ellos. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final CatEstadoRepository estadoRepository;
    private final CatPaisRepository paisRepository;

    public CatEstado obtenerEstado(Short id) {
        return estadoRepository.findById(id).orElseThrow(() ->
                ErrorValidacionException.campo("domicilio.estadoId", "no existe la entidad federativa " + id));
    }

    public void validarPais(String codigo, String campo) {
        if (codigo == null || !paisRepository.existsById(codigo)) {
            throw ErrorValidacionException.campo(campo, "el país '" + codigo + "' no existe en el catálogo ISO 3166-1");
        }
    }

    public List<EstadoResponse> estados() {
        return estadoRepository.findAllByOrderByIdAsc().stream()
                .map(estado -> new EstadoResponse(estado.getId(), estado.getNombre()))
                .toList();
    }

    public List<PaisResponse> paises() {
        return paisRepository.findAllByOrderByNombreAsc().stream()
                .map(pais -> new PaisResponse(pais.getCodigo(), pais.getNombre()))
                .toList();
    }

    public List<OpcionCatalogo> sexos() {
        return opciones(Sexo.values());
    }

    public List<OpcionCatalogo> estadosCiviles() {
        return opciones(EstadoCivil.values());
    }

    public List<OpcionCatalogo> estatusCuenta() {
        return opciones(EstatusCuenta.values());
    }

    private static <E extends Enum<E> & CatalogoEnum> List<OpcionCatalogo> opciones(E[] valores) {
        return Arrays.stream(valores).map(valor -> new OpcionCatalogo(valor.name(), valor.getDescripcion())).toList();
    }
}

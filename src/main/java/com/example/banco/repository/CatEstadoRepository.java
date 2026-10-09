package com.example.banco.repository;

import com.example.banco.entity.CatEstado;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Catálogo de solo lectura: no expone save ni delete. */
public interface CatEstadoRepository extends Repository<CatEstado, Short> {

    Optional<CatEstado> findById(Short id);

    List<CatEstado> findAllByOrderByIdAsc();
}

package com.example.banco.repository;

import com.example.banco.entity.CatPais;
import org.springframework.data.repository.Repository;

import java.util.List;

/** Catálogo de solo lectura: no expone save ni delete. */
public interface CatPaisRepository extends Repository<CatPais, String> {

    boolean existsById(String codigo);

    List<CatPais> findAllByOrderByNombreAsc();
}

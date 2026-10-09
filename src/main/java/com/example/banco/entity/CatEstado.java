package com.example.banco.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Entidad federativa (catálogo INEGI, solo lectura). */
@Entity
@Table(name = "cat_estado")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatEstado {

    @Id
    private Short id;

    @Column(nullable = false, length = 60)
    private String nombre;

    public CatEstado(Short id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}

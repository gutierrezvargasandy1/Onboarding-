package com.example.banco.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** País ISO 3166-1 alfa-2 (catálogo de solo lectura). */
@Entity
@Table(name = "cat_pais")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatPais {

    @Id
    @Column(name = "codigo_iso2", length = 2)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;
}

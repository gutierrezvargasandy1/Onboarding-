package com.example.banco.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

/**
 * Domicilio del cliente (1:1). Comparte la llave primaria con el cliente
 * ({@code @MapsId}): no necesita una columna id propia ni un índice único extra.
 */
@Entity
@Table(name = "domicilios")
@Getter
@Setter
@NoArgsConstructor
public class Domicilio {

    @Id
    @Setter(AccessLevel.NONE)
    private Integer clienteId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    @Setter(AccessLevel.NONE)
    private Cliente cliente;

    @Column(nullable = false, length = 100)
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 10)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 10)
    private String numeroInterior;

    @Column(nullable = false, length = 100)
    private String colonia;

    @Column(nullable = false, length = 100)
    private String municipio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private CatEstado estado;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;

    @Column(nullable = false, length = 2)
    private String pais = "MX";

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion")
    @Setter(AccessLevel.NONE)
    private OffsetDateTime fechaActualizacion;

    void asignarCliente(Cliente propietario) {
        this.cliente = propietario;
    }
}

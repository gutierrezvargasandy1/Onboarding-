package com.example.banco.entity;

import com.example.banco.entity.converter.EstatusCuentaConverter;
import com.example.banco.entity.enums.EstatusCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Cuenta bancaria (N cuentas por cliente). El número lo genera la base de datos y nunca cambia. */
@Entity
@Table(name = "cuentas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Bloqueo optimista: evita actualizaciones perdidas del saldo. */
    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    /** Copia de solo lectura de la llave foránea (evita cargar al cliente para conocer su id). */
    @Column(name = "cliente_id", insertable = false, updatable = false)
    private Integer clienteId;

    /** 9 dígitos de secuencia + dígito verificador Luhn, asignados por la base de datos. */
    @Generated(event = EventType.INSERT)
    @Column(name = "numero_cuenta", length = 10, updatable = false)
    private String numeroCuenta;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Convert(converter = EstatusCuentaConverter.class)
    @Column(name = "estatus_id", nullable = false)
    private EstatusCuenta estatus;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_apertura")
    private OffsetDateTime fechaApertura;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion")
    private OffsetDateTime fechaActualizacion;

    /** Cuenta nueva: asociada al cliente, con el saldo inicial y estatus ACTIVA. */
    public static Cuenta abrir(Cliente cliente, BigDecimal saldoInicial, String moneda) {
        Cuenta cuenta = new Cuenta();
        cuenta.cliente = cliente;
        cuenta.clienteId = cliente.getId();
        cuenta.saldo = saldoInicial;
        cuenta.moneda = moneda;
        cuenta.estatus = EstatusCuenta.ACTIVA;
        return cuenta;
    }

    public boolean estaActiva() {
        return estatus == EstatusCuenta.ACTIVA;
    }

    /** Al dar de baja al cliente, sus cuentas activas pasan a INACTIVA. */
    public void inactivar() {
        if (estaActiva()) {
            estatus = EstatusCuenta.INACTIVA;
        }
    }
}

package com.example.banco.entity;

import com.example.banco.entity.converter.EstadoCivilConverter;
import com.example.banco.entity.converter.SexoConverter;
import com.example.banco.entity.enums.EstadoCivil;
import com.example.banco.entity.enums.Sexo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente persona física. Relaciones: 1:1 con {@link Domicilio}, 1:N con
 * {@link Cuenta} y 1:1 con {@link Usuario} (mapeada desde el usuario).
 * CURP y RFC no tienen setter y no se actualizan (updatable = false).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Integer id;

    /** Bloqueo optimista: dos actualizaciones simultáneas no se pisan. */
    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, length = 50)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 50)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 18, updatable = false)
    @Setter(AccessLevel.NONE)
    private String curp;

    @Column(nullable = false, length = 13, updatable = false)
    @Setter(AccessLevel.NONE)
    private String rfc;

    @Convert(converter = SexoConverter.class)
    @Column(name = "sexo_id", nullable = false)
    private Sexo sexo;

    @Column(nullable = false, length = 2)
    private String nacionalidad;

    @Convert(converter = EstadoCivilConverter.class)
    @Column(name = "estado_civil_id", nullable = false)
    private EstadoCivil estadoCivil;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(name = "telefono_movil", nullable = false, length = 10)
    private String telefonoMovil;

    @Column(name = "telefono_alterno", length = 10)
    private String telefonoAlterno;

    @Column(nullable = false, length = 100)
    private String ocupacion;

    @Column(nullable = false, length = 100)
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean activo = true;

    // Valores que asigna la base de datos (DEFAULT now() y triggers)
    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_registro")
    @Setter(AccessLevel.NONE)
    private OffsetDateTime fechaRegistro;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion")
    @Setter(AccessLevel.NONE)
    private OffsetDateTime fechaActualizacion;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_baja")
    @Setter(AccessLevel.NONE)
    private OffsetDateTime fechaBaja;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL)
    @Setter(AccessLevel.NONE)
    private Domicilio domicilio;

    @OneToMany(mappedBy = "cliente")
    @OrderBy("id ASC")
    @Setter(AccessLevel.NONE)
    private List<Cuenta> cuentas = new ArrayList<>();

    public Cliente(String curp, String rfc) {
        this.curp = curp;
        this.rfc = rfc;
    }

    public void asignarDomicilio(Domicilio nuevoDomicilio) {
        this.domicilio = nuevoDomicilio;
        nuevoDomicilio.asignarCliente(this);
    }

    public void agregarCuenta(Cuenta cuenta) {
        cuentas.add(cuenta);
    }

    /** Baja lógica: el registro se conserva; la BD fija fecha_baja. */
    public void darDeBaja() {
        this.activo = false;
    }

    public String getNombreCompleto() {
        StringBuilder completo = new StringBuilder(nombre);
        if (segundoNombre != null) {
            completo.append(' ').append(segundoNombre);
        }
        return completo.append(' ').append(apellidoPaterno).append(' ').append(apellidoMaterno).toString();
    }
}

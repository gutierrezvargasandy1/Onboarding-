package com.example.banco.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

/**
 * Usuario de acceso (1:1 con el cliente). El nombre de usuario es el correo del
 * cliente y la contraseña se guarda solo como hash BCrypt.
 */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "cliente_id", insertable = false, updatable = false)
    private Integer clienteId;

    @Column(nullable = false, length = 100)
    private String correo;

    /** Hash BCrypt. Nunca se expone en respuestas ni en logs. */
    @Column(nullable = false, length = 100)
    private String password;

    /** Aumenta con cada cambio de contraseña; los JWT con una versión anterior dejan de servir. */
    @Column(name = "version_credenciales", nullable = false)
    private int versionCredenciales;

    @Column(nullable = false)
    private boolean activo = true;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha_creacion")
    private OffsetDateTime fechaCreacion;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha_actualizacion")
    private OffsetDateTime fechaActualizacion;

    /** Usuario nuevo y activo; su nombre de usuario es el correo del cliente. */
    public static Usuario crear(Cliente cliente, String hashPassword) {
        Usuario usuario = new Usuario();
        usuario.cliente = cliente;
        usuario.clienteId = cliente.getId();
        usuario.correo = cliente.getCorreo();
        usuario.password = hashPassword;
        usuario.activo = true;
        return usuario;
    }

    public void cambiarPassword(String nuevoHash) {
        this.password = nuevoHash;
        this.versionCredenciales++;
    }

    public void actualizarCorreo(String nuevoCorreo) {
        this.correo = nuevoCorreo;
    }

    public void inactivar() {
        this.activo = false;
    }

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", clienteId=" + clienteId + ", activo=" + activo + "]";
    }
}

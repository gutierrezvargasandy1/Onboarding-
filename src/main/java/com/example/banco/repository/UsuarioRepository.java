package com.example.banco.repository;

import com.example.banco.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /** Inicio de sesión: el correo es el nombre de usuario (índice único uq_usuarios_correo). */
    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByClienteId(Integer clienteId);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndClienteIdNot(String correo, Integer clienteId);
}

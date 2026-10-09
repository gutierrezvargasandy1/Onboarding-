package com.example.banco.service;

import com.example.banco.dto.request.CambioPasswordRequest;
import com.example.banco.dto.request.CrearUsuarioRequest;
import com.example.banco.dto.response.UsuarioResponse;
import com.example.banco.entity.Cliente;
import com.example.banco.entity.Usuario;
import com.example.banco.exception.AccesoDenegadoException;
import com.example.banco.exception.ClienteNoEncontradoException;
import com.example.banco.exception.PasswordInvalidaException;
import com.example.banco.exception.UsuarioInactivoException;
import com.example.banco.exception.UsuarioNoEncontradoException;
import com.example.banco.mapper.UsuarioMapper;
import com.example.banco.repository.ClienteRepository;
import com.example.banco.repository.UsuarioRepository;
import com.example.banco.security.UsuarioAutenticado;
import com.example.banco.util.PoliticaPassword;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    /**
     * Crea el usuario de acceso del cliente recién registrado: nombre de usuario =
     * correo,
     * contraseña cifrada con BCrypt y estado activo. Corre dentro de la transacción
     * del registro.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Usuario crearUsuario(Cliente cliente, String password) {
        validarPolitica(password);
        return usuarioRepository.save(Usuario.crear(cliente, passwordEncoder.encode(password)));
    }

    @Transactional
    public UsuarioResponse crearUsuarioDesdeDto(CrearUsuarioRequest solicitud) {
        // 1. Obtener la entidad Cliente usando el campo correspondiente de tu DTO
        // Nota: Asegúrate de usar el getter correcto de CrearUsuarioRequest (ej.
        // solicitud.clienteId(), solicitud.getClienteId(), etc.)
        Cliente cliente = clienteRepository.findById(solicitud.clienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        // 2. Invocar el método con Propagation.MANDATORY
        Usuario usuarioCreado = crearUsuario(cliente, solicitud.password());

        // 3. Mapear a respuesta DTO utilizando el usuarioMapper ya inyectado
        return usuarioMapper.aRespuesta(usuarioCreado);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Integer id, UsuarioAutenticado actual) {
        Usuario usuario = buscarPropio(id, actual);
        return usuarioMapper.aRespuesta(usuario);
    }

    @Transactional
    public void cambiarPassword(Integer id, CambioPasswordRequest solicitud, UsuarioAutenticado actual) {
        Usuario usuario = buscarPropio(id, actual);
        if (!usuario.isActivo()) {
            throw new UsuarioInactivoException();
        }
        if (!passwordEncoder.matches(solicitud.passwordActual(), usuario.getPassword())) {
            throw new PasswordInvalidaException("La contraseña actual es incorrecta");
        }
        validarPolitica(solicitud.passwordNueva());
        if (passwordEncoder.matches(solicitud.passwordNueva(), usuario.getPassword())) {
            throw new PasswordInvalidaException("La nueva contraseña debe ser distinta de la actual");
        }
        usuario.cambiarPassword(passwordEncoder.encode(solicitud.passwordNueva()));
        log.info("Usuario {} cambió su contraseña; sus tokens anteriores quedan invalidados", id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void sincronizarCorreo(Cliente cliente) {
        usuarioRepository.findByClienteId(cliente.getId())
                .filter(usuario -> !usuario.getCorreo().equals(cliente.getCorreo()))
                .ifPresent(usuario -> usuario.actualizarCorreo(cliente.getCorreo()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void inactivarPorBajaDeCliente(Integer clienteId) {
        usuarioRepository.findByClienteId(clienteId).ifPresent(Usuario::inactivar);
    }

    private Usuario buscarPropio(Integer id, UsuarioAutenticado actual) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
        if (actual == null || !usuario.getId().equals(actual.usuarioId())) {
            throw new AccesoDenegadoException("Solo puedes consultar o modificar tu propio usuario");
        }
        return usuario;
    }

    private static void validarPolitica(String password) {
        List<String> faltantes = PoliticaPassword.incumplimientos(password);
        if (!faltantes.isEmpty()) {
            throw new PasswordInvalidaException("La contraseña requiere: " + String.join(", ", faltantes));
        }
    }
}
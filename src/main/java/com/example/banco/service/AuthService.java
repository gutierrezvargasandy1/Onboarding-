package com.example.banco.service;

import com.example.banco.dto.request.LoginRequest;
import com.example.banco.dto.response.LoginResponse;
import com.example.banco.entity.Usuario;
import com.example.banco.exception.CredencialesInvalidasException;
import com.example.banco.exception.UsuarioInactivoException;
import com.example.banco.repository.UsuarioRepository;
import com.example.banco.security.IntentosLoginService;
import com.example.banco.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inicio de sesión con correo y contraseña. Valida que el usuario exista, que la
 * contraseña coincida (BCrypt) y que esté activo; entonces emite el JWT.
 */
@Slf4j
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IntentosLoginService intentosLogin;

    /** Hash de referencia: si el correo no existe se compara igual, para que el tiempo de respuesta no lo delate. */
    private final String hashDeReferencia;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, IntentosLoginService intentosLogin) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.intentosLogin = intentosLogin;
        this.hashDeReferencia = passwordEncoder.encode("referencia-" + UUID.randomUUID());
    }

    @Transactional(readOnly = true)
    public LoginResponse iniciarSesion(LoginRequest solicitud) {
        String correo = solicitud.correo();
        intentosLogin.verificarNoBloqueado(correo);

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        boolean passwordCorrecta = passwordEncoder.matches(solicitud.password(),
                usuario != null ? usuario.getPassword() : hashDeReferencia);
        if (usuario == null || !passwordCorrecta) {
            intentosLogin.registrarFallo(correo);
            log.info("Inicio de sesión rechazado: credenciales inválidas");
            throw new CredencialesInvalidasException();
        }
        if (!usuario.isActivo()) {
            log.info("Inicio de sesión rechazado: usuario {} inactivo", usuario.getId());
            throw new UsuarioInactivoException();
        }
        intentosLogin.reiniciar(correo);
        JwtService.TokenEmitido token = jwtService.emitir(usuario);
        log.info("Usuario {} inició sesión", usuario.getId());
        return new LoginResponse(token.valor(), "Bearer", token.segundosVigencia(), token.expiraEn(),
                usuario.getId(), usuario.getClienteId(), usuario.getCorreo());
    }
}

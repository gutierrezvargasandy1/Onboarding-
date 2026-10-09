package com.example.banco.security;

import com.example.banco.entity.Usuario;
import com.example.banco.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte un JWT válido (firma, emisor, audiencia y vigencia ya verificados) en
 * el usuario autenticado. Consulta la base de datos en cada petición para que la
 * baja lógica o un cambio de contraseña invaliden los tokens al instante.
 */
@Component
@RequiredArgsConstructor
public class ConvertidorJwtUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer usuarioId = leerUsuarioId(jwt);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new TokenRechazadoException("El usuario del token ya no existe"));
        if (!usuario.isActivo()) {
            throw new TokenRechazadoException("El usuario está inactivo: el cliente fue dado de baja");
        }
        Object version = jwt.getClaim(JwtService.CLAIM_VERSION_CREDENCIALES);
        if (!(version instanceof Number numero) || numero.intValue() != usuario.getVersionCredenciales()) {
            throw new TokenRechazadoException("La sesión ya no es válida porque la contraseña cambió; inicia sesión de nuevo");
        }
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getClienteId(), usuario.getCorreo());
        return new UsernamePasswordAuthenticationToken(principal, jwt, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }

    private static Integer leerUsuarioId(Jwt jwt) {
        try {
            return Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new TokenRechazadoException("El token no identifica a un usuario válido");
        }
    }
}
